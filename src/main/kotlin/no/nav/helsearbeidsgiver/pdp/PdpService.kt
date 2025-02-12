package no.nav.helsearbeidsgiver.pdp

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.altinn.pdp.PdpClient
import no.nav.helsearbeidsgiver.auth.AltinnAuthClient
import no.nav.helsearbeidsgiver.auth.getPdpToken
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
                    systembruker,
                    orgnr,
                )
            }.getOrDefault(false) // TODO: håndter feil ved å svare status 500 / 502 tilbake til bruker
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

fun lagPdpClient(authClient: AltinnAuthClient): PdpClient {
    val altinn3BaseUrl = Env.getProperty("ALTINN_3_BASE_URL")
    val subscriptionKey = Env.getProperty("SUBSCRIPTION_KEY")
    val altinnImRessurs = Env.getProperty("ALTINN_IM_RESSURS")
    val pdpClient =
        PdpClient(
            altinn3BaseUrl,
            subscriptionKey,
            altinnImRessurs,
            authClient::getPdpToken,
        )
    return pdpClient
}
