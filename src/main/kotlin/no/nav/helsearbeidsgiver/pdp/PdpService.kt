package no.nav.helsearbeidsgiver.pdp

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.altinn.pdp.PdpClient
import no.nav.helsearbeidsgiver.utils.AltinnTokenClient
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class PdpService(
    val pdpClient: PdpClient,
) {
    fun harTilgang(
        orgnr: String,
        systembruker: String,
    ): Boolean =
        runBlocking {
            runCatching {
                pdpClient.systemHarRettighetForOrganisasjon(
                    systembruker,
                    orgnr,
                )
            }.getOrDefault(false)
        }
}

fun lagPdpClient(): PdpClient {
    val subscriptionKey = System.getenv("SUBSCRIPTION_KEY")
    sikkerLogger().info("subscriptionKey: $subscriptionKey")
    val altinnTokenClient = AltinnTokenClient()
    val pdpClient =
        PdpClient(
            "https://platform.tt02.altinn.no",
            subscriptionKey,
            "nav_sykepenger_inntektsmelding-nedlasting",
            altinnTokenClient::getToken,
        )
    return pdpClient
}
