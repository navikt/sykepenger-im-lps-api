package no.nav.helsearbeidsgiver.felles.auth

enum class AuthClientIdentityProvider(
    val verdi: String,
) {
    AZURE_AD("azuread"),
    IDPORTEN("idporten"),
    MASKINPORTEN("maskinporten"),
    TOKEN_X("tokenx"),
}
