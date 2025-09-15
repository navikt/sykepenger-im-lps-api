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
        INNTEKT_AVVIKER_FRA_A_ORDNINGEN(
            "Oppgitt inntekt avviker fra inntekt registrert i a-ordningen. " +
                "Send inntektsmelding på nytt med en gyldig årsak til endring av inntekt.",
        ),
    }
}
