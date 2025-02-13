package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.Innsending.toJson
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InnsendingProducer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.json.toPretty
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.LocalDateTime
import java.util.UUID

class InntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val innsendingProducer: InnsendingProducer,
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
        val mottatt = LocalDateTime.now()
        val kontekstId = UUID.randomUUID()

        val publisert =
            innsendingProducer
                .send(
                    Innsending.Key.EVENT_NAME to Innsending.EventName.API_INNSENDING_STARTET.toJson(),
                    Innsending.Key.KONTEKST_ID to kontekstId.toJson(UuidSerializer),
                    Innsending.Key.DATA to
                        mapOf(
                            Innsending.Key.SKJEMA_INNTEKTSMELDING to skjema.toJson(SkjemaInntektsmelding.serializer()),
                            Innsending.Key.MOTTATT to mottatt.toJson(LocalDateTimeSerializer),
                        ).toJson(),
                ).getOrThrow()
        sikkerLogger().info("Publiserte melding om innsendt skjema:\n${publisert.toPretty()}")
    }
}
