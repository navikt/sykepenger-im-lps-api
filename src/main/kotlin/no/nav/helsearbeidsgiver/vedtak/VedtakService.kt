package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger

class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()

    fun lagreVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        if (unleashFeatureToggles.skalLagreVedtakArbeidsgiver()) {
            vedtakRepository.lagreVedtak(
                LagreVedtak(
                    vedtaksperiodeId = vedtakArbeidsgiverMelding.vedtaksperiodeId,
                    fnr = vedtakArbeidsgiverMelding.foedselsnummer.toString(),
                    orgnr = vedtakArbeidsgiverMelding.organisasjonsnummer.toString(),
                    vedtak = vedtakArbeidsgiverMelding,
                ),
            )
        } else {
            logger.info(
                "Lagrer _ikke_ vedtak for vedtaksperiodeId ${vedtakArbeidsgiverMelding.vedtaksperiodeId} fordi " +
                    "featuretoggle lagre-vedtak-arbeidsgiver er skrudd av.",
            )
        }
    }
}
