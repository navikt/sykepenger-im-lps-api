package no.nav.helsearbeidsgiver.auth

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.felles.auth.AuthClientIdentityProvider

private fun AuthClient.hentAltinnToken(target: String): () -> String {
    val maskinportenTokenGetter = tokenGetter(AuthClientIdentityProvider.MASKINPORTEN, target = target)

    return {
        runBlocking {
            altinnExchange(maskinportenTokenGetter())
        }
    }
}

fun AuthClient.pdpTokenGetter() = hentAltinnToken(Env.getProperty("ALTINN_PDP_SCOPE"))
