package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.EventName
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.slf4j.LoggerFactory

class ImKafkaConsumer(
    private val inntektsmeldingService: InntektsmeldingService,
    private val mottakRepository: MottakRepository,
) : LpsKafkaConsumer {
    private val logger = LoggerFactory.getLogger(ImKafkaConsumer::class.java)
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    override fun handleRecord(record: String) {
        // TODO: gjør dette i en transaksjon og gjør det skikkelig..
        // transaction {
        sikkerLogger.debug("Mottatt event: $record")
        val obj = parseRecord(record)
        if (obj == null) {
            sikkerLogger.warn("Ugyldig event mottatt: $record")
            mottakRepository.opprett(ExposedMottak(inntektsMelding = record, gyldig = false))
            return
        }
        try {
            sikkerLogger.info("Received event: ${obj.eventname}")

            when (obj.eventname) {
                EventName.INNTEKTSMELDING_DISTRIBUERT.toString() -> {
                    if (obj.inntektsmelding != null) {
                        inntektsmeldingService.opprettInntektsmelding(obj.inntektsmelding)
                        mottakRepository.opprett(ExposedMottak(record))
                    } else {
                        sikkerLogger.warn("Ugyldig event - mangler felt inntektsmelding, kan ikke lagre")
                    }
                }
            }
        } catch (e: Exception) {
            sikkerLogger.warn("feil - $e")
        }
    }

    private fun parseRecord(record: String): ImMessage? {
        try {
            return jsonMapper.decodeFromString<ImMessage>(record)
        } catch (e: IllegalArgumentException) {
            sikkerLogger.error("Failed to handle record", e)
            return null
        }
    }

    @Serializable
    data class ImMessage(
        @SerialName("@event_name") val eventname: String,
        val inntektsmelding: Inntektsmelding? = null,
    )
}
