package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import java.util.UUID

data class LagreVedtak(
    val vedtaksperiodeId: UUID,
    val fnr: String,
    val orgnr: String,
    val vedtak: VedtakArbeidsgiverMelding,
)
