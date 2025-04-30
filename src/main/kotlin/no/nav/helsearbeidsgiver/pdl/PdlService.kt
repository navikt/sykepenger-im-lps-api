package no.nav.helsearbeidsgiver.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.felles.auth.AuthClientIdentityProvider.AZURE_AD

interface IPdlService {
    fun hentPersonFulltNavnForSykmelding(fnr: String): String?
}

class IngenPdlService : IPdlService {
    override fun hentPersonFulltNavnForSykmelding(fnr: String): String? = "PDL skrudd av i prod"
}

class PdlService(
    authClient: AuthClient,
) : IPdlService {
    private val pdlUrl = Env.getProperty("PDL_URL")
    private val tokenGetter = authClient.tokenGetter(AZURE_AD, Env.getProperty("PDL_SCOPE"))
    private val sykmeldingPdlClient =
        PdlClient(
            url = pdlUrl,
            getAccessToken = tokenGetter,
            behandlingsgrunnlag = Behandlingsgrunnlag.SYKMELDING,
        )

    override fun hentPersonFulltNavnForSykmelding(fnr: String): String? =
        runBlocking {
            sykmeldingPdlClient
                .personBolk(listOf(fnr))
                ?.firstOrNull()
                ?.navn
                ?.fulltNavn()
        }
}
