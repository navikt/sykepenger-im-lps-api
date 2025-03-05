package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding

data class Innsending(
    val innsendingId: String,
    val orgnr: String,
    val lps: String,
    val fnr: String,
    val status: InnsendingStatus,
    val dokument: SkjemaInntektsmelding,
    val forespoerselId: String,
    val innsendtDato: String,
    val inntektsmeldingId: Int?,
    val feilAarsak: String?,
)
