package no.nav.helsearbeidsgiver.kafka.forespoersel

import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.BehovMessage
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.NotisType
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.logger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class ForespoerselTolker(
    private val forespoerselService: ForespoerselService,
    private val mottakRepository: MottakRepository,
    private val dialogportenService: DialogportenService,
    private val dokumentkoblingService: DokumentkoblingService,
) : MeldingTolker {
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")
    private val logger = logger()

    override fun lesMelding(melding: String) {
        val obj =
            try {
                parseRecord(melding)
            } catch (e: Exception) {
                if (matcherBehovMelding(melding)) {
                    logger.debug("Ignorerer behov-melding")
                    return
                } else {
                    if (melding.contains(NotisType.FORESPOERSEL_FOR_VEDTAKSPERIODE_ID.toString())) {
                        logger.error("import fsp: Ugyldig forespørselformat! Melding  ${NotisType.FORESPOERSEL_FOR_VEDTAKSPERIODE_ID}")
                        sikkerLogger.error("import fsp: Ugyldig forespørselformat! Melding", e)
                        return
                    }
                    sikkerLogger.error("Ugyldig forespørselformat!", e)
                    throw e
                }
            }
        logger.info("Mottatt notis ${obj.notis}")
        val forespoerselId = obj.forespoerselId ?: obj.forespoersel?.forespoerselId
        if (forespoerselId == null) {
            logger().error("forespoerselId er null!")
            sikkerLogger.error("forespoerselId er null! Melding: $melding")
            throw IllegalArgumentException("forespoerselId er null!")
        }
        when (obj.notis) {
            NotisType.FORESPØRSEL_MOTTATT -> {
                val forespoersel = obj.forespoersel
                if (forespoersel != null) {
                    transaction {
                        try {
                            forespoerselService.lagreNyForespoersel(forespoersel)
                            mottakRepository.opprett(ExposedMottak(melding))
                            dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(forespoersel)
                            dokumentkoblingService.produserForespoerselKobling(forespoersel)
                        } catch (e: Exception) {
                            rollback()
                            logger.error("Klarte ikke å lagre forespørsel i database: $forespoerselId")
                            sikkerLogger.error("Klarte ikke å lagre forespørsel i database: $forespoerselId", e)
                            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
                        }
                    }
                } else {
                    logger.error("Ugyldige eller manglende verdier i ${NotisType.FORESPØRSEL_MOTTATT}!")
                    mottakRepository.opprett(ExposedMottak(melding = melding, gyldig = false))
                }
            }

            NotisType.FORESPOERSEL_OPPDATERT -> {
                transaction {
                    try {
                        if (obj.forespoersel != null) {
                            forespoerselService.lagreOppdatertForespoersel(obj)
                            mottakRepository.opprett(ExposedMottak(melding))
                            dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(obj.forespoersel)
                            dokumentkoblingService.produserForespoerselKobling(obj.forespoersel)
                        }
                    } catch (e: Exception) {
                        rollback()
                        logger.error("Klarte ikke å lagre oppdatert forespørsel i database: $forespoerselId")
                        sikkerLogger.error("Klarte ikke å lagre oppdatert forespørsel i database: $forespoerselId", e)
                        throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
                    }
                }
            }

            NotisType.FORESPOERSEL_BESVART -> {
                forespoerselService.settBesvart(forespoerselId)
                mottakRepository.opprett(ExposedMottak(melding))
            }

            NotisType.FORESPOERSEL_BESVART_SIMBA -> {
                forespoerselService.settBesvart(forespoerselId)
                mottakRepository.opprett(ExposedMottak(melding))
            }

            NotisType.FORESPOERSEL_FORKASTET -> {
                forespoerselService.settForkastet(forespoerselId)
                mottakRepository.opprett(ExposedMottak(melding))
            }

            NotisType.FORESPOERSEL_KASTET_TIL_INFOTRYGD -> {
                // TODO:: Skal vi håndtere kastet til infotrygd?
                logger.info("Forespørsel kastet til infotrygd - håndteres ikke")
                mottakRepository.opprett(ExposedMottak(melding, false))
            }

            NotisType.FORESPOERSEL_FOR_VEDTAKSPERIODE_ID -> {
                logger.info("import fsp: Forespørsel for vedtaksperiodeId mottat")
                obj.forespoersel?.let { forespoersel ->
                    logger.info(
                        "import fsp: Lagrer eller oppdaterer forespørsel med id: ${forespoersel.forespoerselId} for vedtaksperiodeId: ${forespoersel.vedtaksperiodeId}",
                    )

                    obj.eksponertForespoerselId?.let { forespoerselService.lagreEllerOppdaterForespoersel(forespoersel, obj.status, it) }
                        ?: logger.error(
                            "import fsp: Eksponert forespørsel ID er null for forespørsel med id: ${forespoersel.forespoerselId}",
                        )
                }
            }
        }
    }

    private fun parseRecord(record: String): PriMessage = jsonMapper.decodeFromString<PriMessage>(record)

    // Melding til / fra Bro med behov-felt er ikke relevant for denne klassen, så vi ignorerer disse
    private fun matcherBehovMelding(record: String): Boolean {
        try {
            jsonMapper.decodeFromString<BehovMessage>(record)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
