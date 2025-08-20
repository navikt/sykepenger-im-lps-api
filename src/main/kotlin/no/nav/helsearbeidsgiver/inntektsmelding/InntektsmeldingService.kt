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

    fun hentInntektsMelding(filter: InntektsmeldingFilter): List<InntektsmeldingResponse> {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for request: $filter")
            inntektsmeldingRepository.hent(filter = filter)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} inntektsmeldinger for request: $filter")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av inntektsmeldinger for request: $filter", it)
        }
        throw RuntimeException("Feil ved henting av inntektsmeldinger for request: $filter")
    }

    fun hentInntektsmeldingMedInnsendingId(innsendingId: UUID): InntektsmeldingResponse? {
        logger().info("Henter inntektsmelding med innsendingId $innsendingId")
        return inntektsmeldingRepository.hentMedInnsendingId(innsendingId = innsendingId)
    }

    fun opprettInntektsmelding(
        im: Inntektsmelding,
        innsendingStatus: InnsendingStatus = InnsendingStatus.GODKJENT,
    ) {
        inntektsmeldingRepository
            .hentMedInnsendingId(innsendingId = im.id, orgnr = im.avsender.orgnr.verdi)
            ?.let {
                sikkerLogger().info("Inntektsmelding med innsendingId ${im.id} finnes allerede")
                sikkerLogger().info("Duplikat inntektsmelding : $im")
                return
            }
        lagreInntektsmelding(im, innsendingStatus)
    }

    private fun lagreInntektsmelding(
        im: Inntektsmelding,
        innsendingStatus: InnsendingStatus,
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
            throw Exception("Feil ved oppretting av inntektsmelding med id: ${im.id}", it)
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
