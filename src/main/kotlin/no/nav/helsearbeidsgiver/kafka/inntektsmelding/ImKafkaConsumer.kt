package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.slf4j.LoggerFactory

class ImKafkaConsumer(
    private val inntektsmeldingService: InntektsmeldingService,
    private val mottakRepository: MottakRepository,
) : LpsKafkaConsumer {
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    override fun handleRecord(record: String) {
        // TODO: gjør dette i en transaksjon og gjør det skikkelig..
        // transaction {
        sikkerLogger.info("Mottatt event: $record")
        val obj = parseRecord(record)
        if (obj == null) {
            sikkerLogger.warn("Ugyldig event mottatt: $record")
            mottakRepository.opprett(ExposedMottak(inntektsMelding = record, gyldig = false))
            return
        }
        try {
            inntektsmeldingService.opprettInntektsmelding(obj.inntektsmeldingV1)
            mottakRepository.opprett(ExposedMottak(record))
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
        val journalpostId: String,
        val inntektsmeldingV1: Inntektsmelding,
    )
}
