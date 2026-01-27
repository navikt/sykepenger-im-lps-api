package no.nav.helsearbeidsgiver.pdp

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.altinn.pdp.PdpClient
import no.nav.helsearbeidsgiver.auth.pdpTokenGetter
import no.nav.helsearbeidsgiver.config.configureAuthClient
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

interface IPdpService {
    fun harTilgang(
        systembruker: String,
        orgnr: String,
        ressurs: String,
    ): Boolean

    fun personHarTilgang(
        fnr: String,
        orgnr: String,
        ressurs: String,
    ): Boolean
}

object PdpService :
    IPdpService {
    val altinn3BaseUrl = Env.getProperty("ALTINN_3_BASE_URL")
    val subscriptionKey = Env.getProperty("SUBSCRIPTION_KEY")
    val authClient = configureAuthClient()

    val pdpClient =
        PdpClient(
            altinn3BaseUrl,
            subscriptionKey,
            authClient.pdpTokenGetter(),
        )

    override fun harTilgang(
        systembruker: String,
        orgnr: String,
        ressurs: String,
    ): Boolean =
        runBlocking {
            sikkerLogger().info("PDP orgnr: $orgnr, systembruker: $systembruker, ressurs: $ressurs")
            runCatching {
                pdpClient.systemHarRettighetForOrganisasjoner(
                    systembrukerId = systembruker,
                    orgnumre = setOf(orgnr),
                    ressurs = ressurs,
                )
            }.getOrDefault(false) // TODO: håndter feil ved å svare status 500/502 tilbake til bruker
        }

    override fun personHarTilgang(
        fnr: String,
        orgnr: String,
        ressurs: String,
    ): Boolean =
        runBlocking {
            sikkerLogger().info("PDP orgnr: $orgnr, fnr: $fnr, ressurs: $ressurs")
            runCatching {
                pdpClient.personHarRettighetForOrganisasjoner(
                    fnr = fnr,
                    orgnumre = setOf(orgnr),
                    ressurs = ressurs,
                )
            }.getOrDefault(false)
        }
}

object LocalhostPdpService : IPdpService {
    override fun harTilgang(
        systembruker: String,
        orgnr: String,
        ressurs: String,
    ): Boolean {
        sikkerLogger().info("Ingen PDP, har tilgang")
        return true
    }

    override fun personHarTilgang(
        fnr: String,
        orgnr: String,
        ressurs: String,
    ): Boolean {
        sikkerLogger().info("Ingen PDP, har tilgang")
        return true
    }
}
