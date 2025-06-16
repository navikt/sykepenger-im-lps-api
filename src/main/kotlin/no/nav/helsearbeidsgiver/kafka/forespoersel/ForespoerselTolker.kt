package no.nav.helsearbeidsgiver.kafka.forespoersel

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
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
import java.util.UUID

class ForespoerselTolker(
    private val forespoerselRepository: ForespoerselRepository,
    private val forespoerselService: ForespoerselService,
    private val mottakRepository: MottakRepository,
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
                            forespoerselService.lagreForespoersel(forespoersel)
                            mottakRepository.opprett(ExposedMottak(melding))
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
                    logger.info("Mottatt oppdatering av forespørsel med id $forespoerselId ")
                    forespoerselService.lagreOppdatertForespoersel(obj)
                    mottakRepository.opprett(ExposedMottak(melding))
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

    // Melding til / fra Bro med behov-felt er ikke relevant for denne klassen, så vi ignorerer disse
    private fun matcherBehovMelding(record: String): Boolean {
        try {
            jsonMapper.decodeFromString<BehovMessage>(record)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun settForkastet(forespoerselId: UUID) {
        val antall = forespoerselRepository.settForkastet(forespoerselId)
        logger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status forkastet")
    }

    private fun settBesvart(forespoerselId: UUID) {
        val antall = forespoerselRepository.settBesvart(forespoerselId)
        logger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status besvart")
    }
}
