package no.nav.helsearbeidsgiver.innsending

import kotlinx.serialization.Serializable

@Serializable
data class Valideringsfeil(
    val feilkode: Feilkode,
    val feilmelding: String? = null, // Spesifikk melding fra Simba
) {
    enum class Feilkode {
        INNTEKT_AVVIKER_FRA_A_ORDNINGEN,
    }
}
