package no.nav.helsearbeidsgiver.pdp

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.altinn.pdp.PdpClient
import no.nav.helsearbeidsgiver.auth.pdpTokenGetter
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

interface IPdpService {
    fun harTilgang(
        systembruker: String,
        orgnr: String,
    ): Boolean
}

class PdpService(
    val pdpClient: PdpClient,
) : IPdpService {
    override fun harTilgang(
        systembruker: String,
        orgnr: String,
    ): Boolean =
        runBlocking {
            sikkerLogger().info("orgnr: $orgnr, systembruker: $systembruker")
            runCatching {
                pdpClient.systemHarRettighetForOrganisasjon(
                    systembrukerId = systembruker,
                    orgnr = orgnr,
                    ressurs = Env.getProperty("ALTINN_IM_RESSURS"),
                )
            }.getOrDefault(false) // TODO: håndter feil ved å svare status 500/502 tilbake til bruker
        }
}

class LocalhostPdpService : IPdpService {
    override fun harTilgang(
        systembruker: String,
        orgnr: String,
    ): Boolean {
        sikkerLogger().info("Ingen PDP, har tilgang")
        return true
    }
}

// Benytter default ingen tilgang i prod inntil vi ønsker å eksponere APIet via http
class IngenTilgangPdpService : IPdpService {
    override fun harTilgang(
        systembruker: String,
        orgnr: String,
    ): Boolean {
        sikkerLogger().info("Ingen PDP, ingen tilgang")
        return false
    }
}

fun lagPdpClient(authClient: AuthClient): PdpClient {
    val altinn3BaseUrl = Env.getProperty("ALTINN_3_BASE_URL")
    val subscriptionKey = Env.getProperty("SUBSCRIPTION_KEY")

    val pdpClient =
        PdpClient(
            altinn3BaseUrl,
            subscriptionKey,
            authClient.pdpTokenGetter(),
        )
    return pdpClient
}
