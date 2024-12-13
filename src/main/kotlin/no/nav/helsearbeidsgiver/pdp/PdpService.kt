package no.nav.helsearbeidsgiver.pdp

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.altinn.pdp.PdpClient
import no.nav.helsearbeidsgiver.utils.AltinnTokenClient

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
    val altinnTokenClient = AltinnTokenClient()
    val pdpClient =
        PdpClient(
            "https://platform.tt02.altinn.no",
            System.getenv("SUBSCRIPTION_KEY"),
            "nav_sykepenger_inntektsmelding-nedlasting",
            altinnTokenClient::getToken,
        )
    return pdpClient
}
