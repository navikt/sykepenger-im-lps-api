package no.nav.helsearbeidsgiver.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.felles.auth.AuthClientIdentityProvider.AZURE_AD
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class PdlService(
    authClient: AuthClient,
) {
    private val pdlUrl = Env.getProperty("PDL_URL")
    private val tokenGetter = authClient.tokenGetter(AZURE_AD, Env.getProperty("PDL_SCOPE"))
    private val sykmeldingPdlClient =
        PdlClient(
            url = pdlUrl,
            behandlingsgrunnlag = Behandlingsgrunnlag.SYKMELDING,
            cacheConfig = LocalCache.Config(entryDuration = 30.minutes, maxEntries = 10_000),
            getAccessToken = tokenGetter,
        )

    fun hentFullPerson(
        fnr: String,
        sykmeldingId: UUID,
    ): FullPerson =
        runBlocking {
            sykmeldingPdlClient
                .personBolk(listOf(fnr))
                .firstOrNull() ?: throw FantIkkePersonException(fnr, sykmeldingId)
        }
}

class FantIkkePersonException(
    val fnr: String,
    val sykmeldingId: UUID,
) : RuntimeException("Fant ikke person i PDL")
