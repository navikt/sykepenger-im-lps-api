package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.json.toPretty
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.UUID

class InntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val kafkaProducer: KafkaProducer<String, JsonElement>,
) {
    fun hentInntektsmeldingerByOrgNr(orgnr: String): InntektsmeldingResponse {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for orgnr: $orgnr")

            inntektsmeldingRepository.hent(orgnr)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} inntektsmeldinger for orgnr: $orgnr")
            return InntektsmeldingResponse(it.size, it)
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av inntektsmeldinger for orgnr: $orgnr", it)
        }
        throw RuntimeException("Feil ved henting av inntektsmeldinger for orgnr: $orgnr")
    }

    fun hentInntektsMeldingByRequest(
        orgnr: String,
        request: InntektsmeldingRequest,
    ): InntektsmeldingResponse {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for request: $request")
            inntektsmeldingRepository.hent(orgNr = orgnr, request = request)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} inntektsmeldinger for request: $request")
            return InntektsmeldingResponse(it.size, it)
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av inntektsmeldinger for request: $request", it)
        }
        throw RuntimeException("Feil ved henting av inntektsmeldinger for request: $request")
    }

    fun opprettInntektsmelding(im: no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding) {
        runCatching {
            sikkerLogger().info("Oppretter inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}")
            inntektsmeldingRepository.opprett(
                im,
                org = im.avsender.orgnr.verdi,
                sykmeldtFnr = im.sykmeldt.fnr.verdi,
                innsendtDato = im.mottatt.toLocalDateTime(),
                forespoerselID = im.type.id.toString(),
            )
        }.onSuccess {
            sikkerLogger().info("Inntektsmelding ${im.type.id} lagret")
        }.onFailure {
            sikkerLogger().warn("Feil ved oppretting av inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}", it)
            throw Exception("Feil ved oppretting av inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}", it)
        }
    }

    fun sendInn(skjema: SkjemaInntektsmelding) {
        sikkerLogger().info("Klar til å sende inn skjema.")

        val mottatt = LocalDateTime.now()

        val publisert =
            sendMessage(
                "@event_name" to "API_INNSENDING_STARTET".toJson(),
                "kontekst_id" to UUID.randomUUID().toJson(UuidSerializer),
                "skjema_inntektsmelding" to skjema.toJson(SkjemaInntektsmelding.serializer()),
                "mottatt" to mottatt.toJson(LocalDateTimeSerializer),
            ).getOrThrow()
        sikkerLogger().info("Publiserte melding om innsendt skjema:\n${publisert.toPretty()}")
    }

    private fun sendMessage(vararg message: Pair<String, JsonElement>): Result<JsonElement> =
        message
            .toMap()
            .toJson()
            .let(::send)

    private fun Map<String, JsonElement>.toJson(): JsonElement =
        toJson(
            MapSerializer(
                String.serializer(),
                JsonElement.serializer(),
            ),
        )

    private fun send(message: JsonElement): Result<JsonElement> =
        message
            .toRecord()
            .runCatching {
                kafkaProducer.send(this).get()
            }.map { message }

    private fun JsonElement.toRecord(): ProducerRecord<String, JsonElement> = ProducerRecord("helsearbeidsgiver.api-innsending", this)
}
