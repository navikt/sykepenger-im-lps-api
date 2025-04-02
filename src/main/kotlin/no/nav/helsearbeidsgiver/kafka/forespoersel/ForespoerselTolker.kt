package no.nav.helsearbeidsgiver.kafka.forespoersel

import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
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
import java.util.UUID

class ForespoerselTolker(
    private val forespoerselRepository: ForespoerselRepository,
    private val mottakRepository: MottakRepository,
    private val dialogportenService: IDialogportenService,
) : MeldingTolker {
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")
    private val logger = logger()

    override fun lesMelding(melding: String) {
        val obj =
            try {
                parseRecord(melding)
            } catch (e: Exception) {
                if (behovMelding(melding)) {
                    logger.debug("Ignorerer behov-melding")
                } else {
                    mottakRepository.opprett(ExposedMottak(melding = melding, gyldig = false))
                }
                return
            }
        obj.notis.let {
            logger.info("Mottatt notis $it")
        }
        val forespoerselId = obj.forespoerselId ?: obj.forespoersel?.forespoerselId
        if (forespoerselId == null) {
            logger().error("forespoerselId er null!")
            mottakRepository.opprett(ExposedMottak(melding, gyldig = false))
            return
        }
        when (obj.notis) {
            NotisType.FORESPØRSEL_MOTTATT -> {
                val forespoersel = obj.forespoersel
                if (forespoersel != null) {
                    transaction {
                        try {
                            forespoerselRepository.lagreForespoersel(
                                forespoerselId = forespoerselId,
                                payload = forespoersel,
                            )
                            mottakRepository.opprett(ExposedMottak(melding))
                        } catch (e: Exception) {
                            rollback()
                            sikkerLogger.error("Klarte ikke å lagre i database!", e)
                            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
                        }
                    }
                    dialogportenService.opprettDialog(
                        orgnr = forespoersel.orgnr,
                        forespoerselId = forespoersel.forespoerselId,
                    )
                } else {
                    sikkerLogger.warn("Ugyldige eller manglende verdier i ${NotisType.FORESPØRSEL_MOTTATT}!")
                    mottakRepository.opprett(ExposedMottak(melding = melding, gyldig = false))
                }
            }

            NotisType.FORESPOERSEL_BESVART -> {
                settBesvart(forespoerselId)
                mottakRepository.opprett(ExposedMottak(melding))
            }

            NotisType.FORESPOERSEL_BESVART_SIMBA -> {
                settBesvart(forespoerselId)
                mottakRepository.opprett(ExposedMottak(melding))
            }

            NotisType.FORESPOERSEL_FORKASTET -> {
                settForkastet(forespoerselId)
                mottakRepository.opprett(ExposedMottak(melding))
            }

            NotisType.FORESPOERSEL_KASTET_TIL_INFOTRYGD -> {
                // TODO:: Skal vi håndtere kastet til infotrygd?
                logger.info("Forespørsel kastet til infotrygd - håndteres ikke")
                mottakRepository.opprett(ExposedMottak(melding, false))
            }
        }
    }

    private fun parseRecord(record: String): PriMessage = jsonMapper.decodeFromString<PriMessage>(record)

    private fun behovMelding(record: String): Boolean {
        try {
            jsonMapper.decodeFromString<BehovMessage>(record)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun settForkastet(forespoerselId: UUID) {
        val antall = forespoerselRepository.settForkastet(forespoerselId)
        sikkerLogger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status forkastet")
    }

    private fun settBesvart(forespoerselId: UUID) {
        val antall = forespoerselRepository.settBesvart(forespoerselId)
        sikkerLogger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status besvart")
    }
}
