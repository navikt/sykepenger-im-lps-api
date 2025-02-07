package no.nav.helsearbeidsgiver.kafka.forespoersel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

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

            when (obj.notis) {
                NotisType.FORESPØRSEL_MOTTATT -> {
                    val forespoersel = obj.forespoersel
                    if (forespoersel != null) {
                        transaction {
                            try {
                                forespoerselRepository.lagreForespoersel(
                                    forespoerselId = forespoersel.forespoerselId.toString(),
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
                    settBesvart(obj.forespoerselId.toString())
                    mottakRepository.opprett(ExposedMottak(melding))
                }

                NotisType.FORESPOERSEL_BESVART_SIMBA -> {
                    settBesvart(obj.forespoerselId.toString())
                    mottakRepository.opprett(ExposedMottak(melding))
                }

                NotisType.FORESPOERSEL_FORKASTET -> {
                    settForkastet(obj.forespoerselId.toString())
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

    private fun settForkastet(forespoerselId: String) {
        if (forespoerselId.isEmpty()) {
            sikkerLogger.warn("ingen forespørselID")
        } else {
            val antall = forespoerselRepository.settForkastet(forespoerselId)
            sikkerLogger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status forkastet")
        }
    }

    private fun settBesvart(forespoerselId: String) {
        if (forespoerselId.isEmpty()) {
            sikkerLogger.warn("ingen forespørselID")
        } else {
            val antall = forespoerselRepository.settBesvart(forespoerselId)
            sikkerLogger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status besvart")
        }
    }

    @Serializable
    data class PriMessage(
        @SerialName("notis") val notis: NotisType,
        @SerialName("forespoersel") val forespoersel: ForespoerselDokument? = null,
        @SerialName("forespoerselId") val forespoerselId: String? = null,
    )

    @Serializable
    enum class NotisType {
        FORESPØRSEL_MOTTATT,
        FORESPOERSEL_BESVART,
        FORESPOERSEL_BESVART_SIMBA,
        FORESPOERSEL_FORKASTET,
        FORESPOERSEL_KASTET_TIL_INFOTRYGD,
    }
}
