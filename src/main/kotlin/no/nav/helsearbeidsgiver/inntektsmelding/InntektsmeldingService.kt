package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

class InntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    fun hentNyesteInntektsmeldingByNavReferanseId(navReferanseId: UUID): InntektsmeldingResponse? {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for forespoerelId: $navReferanseId")
            inntektsmeldingRepository.hent(navReferanseId).maxByOrNull { it.innsendtTid }
        }.onSuccess {
            sikkerLogger().info("Hentet siste Inntektsmelding for forespoerselId: $navReferanseId")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av siste inntektsmelding for forespoerselId: $navReferanseId", it)
        }
        throw RuntimeException("Feil ved henting av siste inntektsmelding for forespoerselId: $navReferanseId")
    }

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
        im: Inntektsmelding,
        innsendingStatus: InnsendingStatus = InnsendingStatus.GODKJENT,
    ) {
        runCatching {
            sikkerLogger().info("Oppretter inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}")
            inntektsmeldingRepository.opprettInntektsmelding(
                im = im,
                innsendingStatus = innsendingStatus,
            )
        }.onSuccess {
            sikkerLogger().info("InnsendtInntektsmelding ${im.type.id} lagret")
        }.onFailure {
            sikkerLogger().warn("Feil ved oppretting av inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}", it)
            throw Exception("Feil ved oppretting av inntektsmelding for orgnr: ${im.avsender.orgnr.verdi}", it)
        }
    }

    fun oppdaterStatus(
        inntektsmelding: Inntektsmelding,
        status: InnsendingStatus,
    ) {
        val antall = inntektsmeldingRepository.oppdaterStatus(inntektsmelding, status)
        logger().info("Oppdaterte ${inntektsmelding.id} med status: $status - antall rader: $antall")
    }
}
