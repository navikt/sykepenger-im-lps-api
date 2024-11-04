package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding.Type
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class InntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    val jsonMapper =
        Json {
            jsonConfig
            ignoreUnknownKeys = true
        }

    fun hentInntektsmeldingerByOrgNr(orgnr: String): List<Inntektsmelding> {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for orgnr: $orgnr")

            inntektsmeldingRepository.hent(orgnr)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} inntektsmeldinger for orgnr: $orgnr")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av inntektsmeldinger for orgnr: $orgnr", it)
            return emptyList()
        }

        return emptyList()
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
            return InntektsmeldingResponse(0, emptyList())
        }

        return InntektsmeldingResponse(0, emptyList())
    }

    fun opprettInntektsmelding(im: no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding) {
        runCatching {
            sikkerLogger().info("Oppretter inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}")
            inntektsmeldingRepository.opprett(
                im =
                    jsonMapper.encodeToString(
                        no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
                            .serializer(),
                        im,
                    ),
                org = im.avsender.orgnr.verdi,
                sykmeldtFnr = im.sykmeldt.fnr.verdi,
                innsendtDato = im.mottatt.toLocalDateTime(),
                forespoerselID =
                    if (im.type.equals(
                            Type.Forespurt,
                        )
                    ) {
                        im.id.toString()
                    } else {
                        null
                    },
            )
        }.onSuccess {
            sikkerLogger().info("Opprettet inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}")
        }.onFailure {
            sikkerLogger().warn("Feil ved oppretting av inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}", it)
        }
    }
}
