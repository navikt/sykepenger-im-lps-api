package no.nav.helsearbeidsgiver.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.config.getPdpService
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v3.TokenValidationContextPrincipal

suspend fun RoutingContext.tokenValidationContext(): TokenValidationContext {
    val principal = call.principal<TokenValidationContextPrincipal>()
    val tokenValidationContext = principal?.context
    if (tokenValidationContext == null) {
        call.respond(HttpStatusCode.Unauthorized, "Uautorisert tilgang")
        throw IllegalStateException("Teknisk feil - mangler tokenValidationContext")
    }
    return tokenValidationContext
}

fun TokenValidationContext.getAuthDetails() = this.getClaims("maskinporten").get("authorization_details") as List<Map<String, String>>

fun TokenValidationContext.getSystembrukerOrgnr(): String {
    val authorizationDetails = this.getAuthDetails()
    val systemBrukerOrgMap = authorizationDetails.first().get("systemuser_org") as Map<String, String>
    val systemBrukerOrgnr = systemBrukerOrgMap.extractOrgnummer()
    require(systemBrukerOrgnr != null)
    return systemBrukerOrgnr
}

fun TokenValidationContext.getConsumerOrgnr(): String {
    val consumer = this.getClaims("maskinporten").get("consumer") as Map<String, String>
    val orgnr = consumer.extractOrgnummer()
    require(orgnr != null)
    return orgnr
}

fun TokenValidationContext.getSystembrukerId(): String {
    val authDetails = this.getAuthDetails()
    val systemBrukerIdListe = authDetails.first().get("systemuser_id") as List<String>
    return systemBrukerIdListe.first()
}

fun TokenValidationContext.gyldigSystembrukerOgConsumer(): Boolean {
    val systembrukerOrgnr = this.getSystembrukerOrgnr()
    val consumerOrgnr = this.getConsumerOrgnr()
    return consumerOrgnr.gyldigOrgnr() &&
        systembrukerOrgnr.gyldigOrgnr()
}

fun TokenValidationContext.harTilgangTilRessurs(ressurs: String): Boolean {
    val systembruker = this.getSystembrukerId()
    val systembrukerOrgnr = this.getSystembrukerOrgnr()
    return getPdpService().harTilgang(systembruker, systembrukerOrgnr, ressurs)
}

fun String.gyldigOrgnr(): Boolean = this.matches(Regex("\\d{9}"))

fun TokenValidationContext.gyldigScope(): Boolean =
    // Kan endre denne tilbake til å sjekke på eksakt match dersom vi bare benytter ett scope
    Env.getPropertyAsList("maskinporten.eksponert_scopes").contains(this.getClaims("maskinporten").get("scope").toString())

private fun Map<String, String>.extractOrgnummer(): String? =
    get("ID")
        ?.split(":")
        ?.get(1)
