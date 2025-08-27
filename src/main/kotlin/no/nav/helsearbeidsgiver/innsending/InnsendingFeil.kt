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
        DIFFERANSE_MOT_AINNTEKT_UTEN_OPPGITT_AARSAK(
            "Det er en differanse mellom inntekten i inntektsmeldingen og inntekten registrert i A-inntekt " +
                "uten at det er oppgitt en årsak. Send inntektsmelding på nytt med en gyldig årsak til endring av inntekten.",
        ),
    }
}
