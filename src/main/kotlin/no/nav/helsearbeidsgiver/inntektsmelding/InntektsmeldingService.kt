package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class InntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    fun hentInntektsmeldingerByOrgNr(orgnr: String): InntektsmeldingFilterResponse {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for orgnr: $orgnr")

            inntektsmeldingRepository.hent(orgnr)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} inntektsmeldinger for orgnr: $orgnr")
            return InntektsmeldingFilterResponse(it.size, it)
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av inntektsmeldinger for orgnr: $orgnr", it)
        }
        throw RuntimeException("Feil ved henting av inntektsmeldinger for orgnr: $orgnr")
    }

    fun hentInntektsMeldingByRequest(
        orgnr: String,
        request: InntektsmeldingFilterRequest,
    ): InntektsmeldingFilterResponse {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for request: $request")
            inntektsmeldingRepository.hent(orgNr = orgnr, request = request)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} inntektsmeldinger for request: $request")
            return InntektsmeldingFilterResponse(it.size, it)
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av inntektsmeldinger for request: $request", it)
        }
        throw RuntimeException("Feil ved henting av inntektsmeldinger for request: $request")
    }

    fun opprettInntektsmelding(
        im: no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding,
        systemNavn: String = "NAV_NO_SIMBA",
        systemVersjon: String = "1.0",
        innsendingStatus: InnsendingStatus = InnsendingStatus.GODKJENT,
    ) {
        runCatching {
            sikkerLogger().info("Oppretter inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}")
            inntektsmeldingRepository.opprettInntektsmelding(
                im = im,
                org = im.avsender.orgnr.verdi,
                sykmeldtFnr = im.sykmeldt.fnr.verdi,
                innsendtDato = im.mottatt.toLocalDateTime(),
                forespoerselID = im.type.id.toString(),
                systemNavn = systemNavn,
                systemVersjon = systemVersjon,
                innsendingStatus = innsendingStatus,
            )
        }.onSuccess {
            sikkerLogger().info("InnsendtInntektsmelding ${im.type.id} lagret")
        }.onFailure {
            sikkerLogger().warn("Feil ved oppretting av inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}", it)
            throw Exception("Feil ved oppretting av inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}", it)
        }
    }
}
