package no.nav.helsearbeidsgiver.kafka.forespoersel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.slf4j.LoggerFactory

class ForespoerselKafkaConsumer(
    private val forespoerselRepository: ForespoerselRepository,
    private val mottakRepository: MottakRepository,
) : LpsKafkaConsumer {
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    override fun handleRecord(record: String) {
        // transaction {
        sikkerLogger.info("Mottatt event: $record")
        val obj = parseRecord(record)
        if (obj == null) {
            sikkerLogger.warn("Ugyldig event mottatt: $record")
            mottakRepository.opprett(ExposedMottak(inntektsMelding = record, gyldig = false))
            return
        }
        try {
            sikkerLogger.info("Received notis: ${obj.notis}")

            when (obj.notis) {
                NotisType.FORESPØRSEL_MOTTATT -> {
                    val forespoersel = obj.forespoersel
                    if (forespoersel != null) {
                        forespoerselRepository.lagreForespoersel(
                            forespoerselId = forespoersel.forespoerselId.toString(),
                            payload = forespoersel,
                        )
                        mottakRepository.opprett(ExposedMottak(record))
                    } else {
                        sikkerLogger.warn("Ugyldige verdier, kan ikke lagre!")
                    }
                }

                NotisType.FORESPOERSEL_BESVART -> {
                    settBesvart(obj.forespoerselId.toString())
                    mottakRepository.opprett(ExposedMottak(record))
                }
                NotisType.FORESPOERSEL_BESVART_SIMBA -> {
                    settBesvart(obj.forespoerselId.toString())
                    mottakRepository.opprett(ExposedMottak(record))
                }
                NotisType.FORESPOERSEL_FORKASTET -> {
                    settForkastet(obj.forespoerselId.toString())
                    mottakRepository.opprett(ExposedMottak(record))
                }
                NotisType.FORESPOERSEL_KASTET_TIL_INFOTRYGD -> {
                    // TODO:: Skal vi håndtere kastet til infotrygd?
                    sikkerLogger.info("Forespørsel kastet til infotrygd - håndteres ikke")
                    mottakRepository.opprett(ExposedMottak(record, false))
                }
                null -> {
                    sikkerLogger.error("Ikke gyldig payload")
                    mottakRepository.opprett(ExposedMottak(record, false))
                }
            }
        } catch (e: Exception) {
            sikkerLogger.warn("feil - $e")
        }
    }

    private fun parseRecord(record: String): PriMessage? {
        try {
            return jsonMapper.decodeFromString<PriMessage>(record)
        } catch (e: IllegalArgumentException) {
            sikkerLogger.error("Failed to handle record", e)
            return null
        }
    }

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
        @SerialName("notis") val notis: NotisType? = null,
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
