package no.nav.helsearbeidsgiver.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.felles.auth.AuthClientIdentityProvider.AZURE_AD
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson

interface IPdlService {
    fun hentFullPerson(fnr: String): FullPerson
}

class IngenPdlService : IPdlService {
    override fun hentFullPerson(fnr: String): FullPerson {
        TODO("Not yet implemented")
    }
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

    override fun hentFullPerson(fnr: String): FullPerson =
        runBlocking {
            sykmeldingPdlClient
                .personBolk(listOf(fnr))
                ?.firstOrNull() ?: throw RuntimeException("Fant ikke person i pdl")
        }
}
