package no.nav.helsearbeidsgiver.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.felles.auth.AuthClientIdentityProvider.AZURE_AD
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import java.util.UUID

class PdlService(
    authClient: AuthClient,
) {
    private val pdlUrl = Env.getProperty("PDL_URL")
    private val tokenGetter = authClient.tokenGetter(AZURE_AD, Env.getProperty("PDL_SCOPE"))
    private val sykmeldingPdlClient =
        PdlClient(
            url = pdlUrl,
            getAccessToken = tokenGetter,
            behandlingsgrunnlag = Behandlingsgrunnlag.SYKMELDING,
        )

    fun hentFullPerson(
        fnr: String,
        sykmeldingId: UUID,
    ): FullPerson =
        runBlocking {
            sykmeldingPdlClient
                .personBolk(listOf(fnr))
                ?.firstOrNull() ?: throw FantIkkePersonException(fnr, sykmeldingId)
        }
}

class FantIkkePersonException(
    val fnr: String,
    val sykmeldingId: UUID,
) : RuntimeException("Fant ikke person i PDL")
