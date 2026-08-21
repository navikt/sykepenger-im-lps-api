package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding

class VedtakService(
    private val vedtakRepository: VedtakRepository,
) {
    fun lagreVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        vedtakRepository.lagreVedtak(
            LagreVedtak(
                vedtaksperiodeId = vedtakArbeidsgiverMelding.vedtaksperiodeId,
                fnr = vedtakArbeidsgiverMelding.foedselsnummer.toString(),
                orgnr = vedtakArbeidsgiverMelding.organisasjonsnummer.toString(),
                vedtak = vedtakArbeidsgiverMelding,
            ),
        )
    }
}
