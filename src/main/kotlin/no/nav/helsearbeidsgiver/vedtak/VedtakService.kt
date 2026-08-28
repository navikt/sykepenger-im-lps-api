package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.kafka.sis.Dokument
import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val dokumentkoblingService: DokumentkoblingService,
) {
    private val logger = logger()

    fun lagreVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        if (unleashFeatureToggles.skalLagreVedtakArbeidsgiver()) {
            val vedtakId =
                vedtakRepository.lagreVedtak(
                    vedtaksperiodeId = vedtakArbeidsgiverMelding.vedtaksperiodeId,
                    fnr = vedtakArbeidsgiverMelding.foedselsnummer,
                    orgnr = vedtakArbeidsgiverMelding.organisasjonsnummer,
                    vedtak = vedtakArbeidsgiverMelding,
                )

            if (vedtakArbeidsgiverMelding.harArbeidsgiverOensketRefusjon) {
                produserVedtakKobling(vedtakId, vedtakArbeidsgiverMelding)
            } else {
                logger.info(
                    "Sender _ikke_ melding på helsearbeidsgiver.dokument-kobling for vedtak med vedtaksperiodeId " +
                        "${vedtakArbeidsgiverMelding.vedtaksperiodeId}, fordi arbeidsgiver ikke har ønsket refusjon.",
                )
            }
        } else {
            logger.info(
                "Lagrer _ikke_ vedtak for vedtaksperiodeId ${vedtakArbeidsgiverMelding.vedtaksperiodeId} fordi " +
                    "featuretoggle lagre-vedtak-arbeidsgiver er skrudd av.",
            )
        }
    }

    private fun produserVedtakKobling(
        vedtakId: UUID,
        vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding,
    ) {
        val vedtaksperiodeId = vedtakArbeidsgiverMelding.vedtaksperiodeId
        val sykmeldingId = finnSykmeldingId(vedtakArbeidsgiverMelding.dokumenter, vedtaksperiodeId)
        val inntektsmeldingId = finnInntektsmeldingId(vedtakArbeidsgiverMelding.dokumenter, vedtaksperiodeId)

        if (sykmeldingId == null || inntektsmeldingId == null) {
            logger.warn(
                "Mangler sykmeldingId og/eller inntektsmeldingId for vedtak med vedtaksperiodeId " +
                    "$vedtaksperiodeId (sykmeldingId=$sykmeldingId, inntektsmeldingId=$inntektsmeldingId), " +
                    "sender ikke melding på helsearbeidsgiver.dokument-kobling.",
            )
            return
        }

        dokumentkoblingService.produserVedtakKobling(
            vedtakId = vedtakId,
            sykmeldingId = sykmeldingId,
            inntektsmeldingId = inntektsmeldingId,
            orgnr = vedtakArbeidsgiverMelding.organisasjonsnummer,
        )
    }

    private fun finnSykmeldingId(
        dokumenter: List<Dokument>,
        vedtaksperiodeId: UUID,
    ): UUID? {
        val sykmeldinger = dokumenter.filter { it.type == Dokument.Type.Sykmelding }
        if (sykmeldinger.size > 1) {
            logger.warn(
                "Fant ${sykmeldinger.size} sykmeldinger for vedtak med vedtaksperiodeId $vedtaksperiodeId, " +
                    "bruker den første i dokumentkoblingen.",
            )
        }
        return sykmeldinger.firstOrNull()?.dokumentId
    }

    private fun finnInntektsmeldingId(
        dokumenter: List<Dokument>,
        vedtaksperiodeId: UUID,
    ): UUID? {
        val inntektsmeldinger = dokumenter.filter { it.type == Dokument.Type.Inntektsmelding }
        if (inntektsmeldinger.size < 2) {
            return inntektsmeldinger.firstOrNull()?.dokumentId
        }

        logger.warn(
            "Fant ${inntektsmeldinger.size} inntektsmeldinger for vedtak med vedtaksperiodeId $vedtaksperiodeId, " +
                "bruker den nyeste (basert på innsendtTid) i dokumentkoblingen.",
        )
        return inntektsmeldinger
            .mapNotNull { inntektsmeldingRepository.hentMedInnsendingId(it.dokumentId) }
            .maxByOrNull { it.innsendtTid }
            ?.id
    }
}
