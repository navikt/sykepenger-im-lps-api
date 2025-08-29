package no.nav.helsearbeidsgiver.innsending

import kotlinx.serialization.Serializable

@Serializable
data class InnsendingFeil(
    val feilkode: Feilkode,
    val feilmelding: String,
) {
    enum class Feilkode(
        val feilmelding: String,
    ) {
        INNTEKTSDIFFERANSE_A_ORDNINGEN_MANGLER_AARSAK(
            "Oppgitt inntekt matcher ikke beløp registrert i A-ordningen. " +
                "Send inntektsmelding på nytt med en gyldig årsak til inntektsendring.",
        ),
    }
}
