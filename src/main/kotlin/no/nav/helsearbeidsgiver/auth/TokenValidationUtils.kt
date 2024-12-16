package no.nav.helsearbeidsgiver.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.helsearbeidsgiver.Env
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

suspend fun PipelineContext<Unit, ApplicationCall>.tokenValidationContext(): TokenValidationContext {
    val principal = call.principal<TokenValidationContextPrincipal>()
    val tokenValidationContext = principal?.context
    if (tokenValidationContext == null) {
        call.respond(HttpStatusCode.Unauthorized, "TokenValidationContext not found")
        throw IllegalStateException("TokenValidationContext not found")
    }
    return tokenValidationContext
}

fun TokenValidationContext.getAuthDetails() = this.getClaims("maskinporten").get("authorization_details") as List<Map<String, String>>

fun TokenValidationContext.getSystembrukerOrgnr(): String? {
    val authorizationDetails = this.getAuthDetails()
    val systemBrukerOrgMap = authorizationDetails.first().get("systemuser_org") as Map<String, String>
    return systemBrukerOrgMap.extractOrgnummer()
}

fun TokenValidationContext.getConsumerOrgnr(): String? {
    val consumer = this.getClaims("maskinporten").get("consumer") as Map<String, String>
    return consumer.extractOrgnummer()
}

fun TokenValidationContext.getSystembrukerId(): String {
    val authDetails = this.getAuthDetails()
    val systemBrukerIdListe = authDetails.first().get("systemuser_id") as List<String>
    return systemBrukerIdListe.first()
}

fun TokenValidationContext.gyldigSystembrukerOgConsumer(harTilgang: (systembruker: String, orgnr: String) -> Boolean): Boolean {
    val systembrukerOrgnr = this.getSystembrukerOrgnr()
    val systembruker = this.getSystembrukerId()
    val consumerOrgnr = this.getConsumerOrgnr()
    return consumerOrgnr != null &&
        consumerOrgnr.matches(Regex("\\d{9}")) &&
        systembrukerOrgnr != null &&
        systembrukerOrgnr.matches(Regex("\\d{9}")) &&
        harTilgang(systembruker, systembrukerOrgnr)
}

fun TokenValidationContext.gyldigScope(): Boolean =
    this.getClaims("maskinporten").get("scope") == Env.getProperty("maskinporten.eksponert_scopes")

private fun Map<String, String>.extractOrgnummer(): String? =
    get("ID")
        ?.split(":")
        ?.get(1)
