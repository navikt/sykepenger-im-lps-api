package no.nav.helsearbeidsgiver.kafka.forespoersel

import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
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

    override fun lesMelding(melding: String) {
        sikkerLogger.info("Mottatt event: $melding")
        val obj =
            try {
                parseRecord(melding)
            } catch (e: Exception) {
                sikkerLogger.info("Ugyldig event, ignorerer melding")
                mottakRepository.opprett(ExposedMottak(melding = melding, gyldig = false))
                return
            }
        try {
            sikkerLogger.info("Mottatt notis: ${obj.notis}")

            val forespoerselId = obj.forespoerselId ?: obj.forespoersel?.forespoerselId
            if (forespoerselId == null) {
                logger().info("forespoerselId er null!")
                mottakRepository.opprett(ExposedMottak(melding))
                return
            }
            when (obj.notis) {
                NotisType.FORESPØRSEL_MOTTATT -> {
                    val forespoersel = obj.forespoersel
                    if (forespoersel != null) {
                        transaction {
                            try {
                                forespoerselRepository.lagreForespoersel(
                                    navReferanseId = forespoersel.forespoerselId,
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
                    sikkerLogger.info("Forespørsel kastet til infotrygd - håndteres ikke")
                    mottakRepository.opprett(ExposedMottak(melding, false))
                }
            }
        } catch (e: Exception) {
            sikkerLogger.warn("feil - $e")
            throw e // sørg for at vi ikke committer offset på kafka ved ikke-håndterte feil
        }
    }

    private fun parseRecord(record: String): PriMessage = jsonMapper.decodeFromString<PriMessage>(record)

    private fun settForkastet(forespoerselId: UUID) {
        val antall = forespoerselRepository.settForkastet(forespoerselId)
        sikkerLogger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status forkastet")
    }

    private fun settBesvart(forespoerselId: UUID) {
        val antall = forespoerselRepository.settBesvart(forespoerselId)
        sikkerLogger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status besvart")
    }
}
