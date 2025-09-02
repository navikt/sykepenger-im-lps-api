package no.nav.helsearbeidsgiver.innsending

import kotlinx.serialization.Serializable

@Serializable
data class Valideringsfeil(
    val feilkode: Feilkode,
    val feilmelding: String,
) {
    enum class Feilkode(
        val feilmelding: String,
    ) {
        INNTEKT_A_ORDNINGEN_AVVIK_MANGLER_AARSAK(
            "Oppgitt inntekt matcher ikke beløp registrert i A-ordningen. " +
                "Send inntektsmelding på nytt med en gyldig årsak til inntektsendring.",
        ),
    }
}
