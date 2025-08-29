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
        INNTEKT_A_ORDNINGEN_AVVIK_MANGLER_AARSAK(
            "Det er en differanse mellom inntekten i inntektsmeldingen og inntekten registrert hos Skatteetaten gjennom A-ordningen " +
                "uten at det er oppgitt en årsak i inntektsmeldingen. Send inntektsmelding på nytt med en gyldig årsak til endring av inntekten.",
        ),
    }
}
