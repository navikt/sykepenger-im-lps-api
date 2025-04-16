package no.nav.helsearbeidsgiver.pdl

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.tokenprovider.OAuth2Environment
import no.nav.helsearbeidsgiver.tokenprovider.oauth2ClientCredentialsTokenGetter

val pdlUrl = Env.getProperty("PDL_URL")
val oauth2Environment =
    OAuth2Environment(
        scope = Env.getProperty("PDL_SCOPE"),
        wellKnownUrl = Env.getProperty("AZURE_APP_WELL_KNOWN_URL"),
        tokenEndpointUrl = Env.getProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        clientId = Env.getProperty("AZURE_APP_CLIENT_ID"),
        clientSecret = Env.getProperty("AZURE_APP_CLIENT_SECRET"),
        clientJwk = Env.getProperty("AZURE_APP_JWK"),
    )

class PdlService {
    private fun opprettPdlClient(behandlingsgrunnlag: Behandlingsgrunnlag) =
        PdlClient(
            url = pdlUrl,
            getAccessToken = oauth2ClientCredentialsTokenGetter(oauth2Environment),
            behandlingsgrunnlag = behandlingsgrunnlag,
        )

    suspend fun hentPersonFulltNavn(
        fnr: String,
        behandlingsgrunnlag: Behandlingsgrunnlag,
    ): String =
        opprettPdlClient(behandlingsgrunnlag)
            .personBolk(listOf(fnr))
            ?.firstOrNull()
            ?.navn
            ?.fulltNavn()
            ?: throw RuntimeException("Fant ikke person i PDL oppslag [fnr:$fnr]")
}
