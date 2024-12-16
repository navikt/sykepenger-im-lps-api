package no.nav.helsearbeidsgiver.pdp

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.altinn.pdp.PdpClient
import no.nav.helsearbeidsgiver.utils.AltinnAuthClient
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class PdpService(
    val pdpClient: PdpClient,
) {
    fun harTilgang(
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

fun lagPdpClient(): PdpClient {
    val altinn3Url = System.getenv("ALTINN_3_URL")
    val subscriptionKey = System.getenv("SUBSCRIPTION_KEY")
    val altinnImRessurs = System.getenv("ALTINN_IM_RESSURS")
    val altinnAuthClient = AltinnAuthClient()
    val pdpClient =
        PdpClient(
            altinn3Url,
            subscriptionKey,
            altinnImRessurs,
            altinnAuthClient::getToken,
        )
    return pdpClient
}
