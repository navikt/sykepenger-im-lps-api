package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class ImKafkaConsumer(
    private val inntektsmeldingService: InntektsmeldingService,
    private val mottakRepository: MottakRepository,
) : LpsKafkaConsumer {
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    override fun handleRecord(record: String) {
        sikkerLogger.info("Mottatt IM: $record")
        try {
            parseRecord(record)
        } catch (e: Exception) {
            sikkerLogger.info("Ugyldig IM, ignorerer melding")
            mottakRepository.opprett(ExposedMottak(melding = record, gyldig = false))
            return
        }
        val obj = parseRecord(record)
        transaction {
            try {
                inntektsmeldingService.opprettInntektsmelding(obj.inntektsmeldingV1)
                mottakRepository.opprett(ExposedMottak(record))
            } catch (e: Exception) {
                rollback()
                sikkerLogger.error("Klarte ikke å lagre i database!", e)
                throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
            }
        }
    }

    private fun parseRecord(record: String): ImMessage = jsonMapper.decodeFromString<ImMessage>(record)

    @Serializable
    data class ImMessage(
        val journalpostId: String,
        val inntektsmeldingV1: Inntektsmelding,
    )
}
