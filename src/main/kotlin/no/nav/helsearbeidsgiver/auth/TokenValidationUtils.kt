package no.nav.helsearbeidsgiver.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
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

fun TokenValidationContext.getSupplierOrgnr(): String? {
    val supplier = this.getClaims("maskinporten").get("supplier") as Map<String, String>
    return supplier.extractOrgnummer()
}

fun TokenValidationContext.getConsumerOrgnr(): String? {
    val consumer = this.getClaims("maskinporten").get("consumer") as Map<String, String>
    return consumer.extractOrgnummer()
}

fun TokenValidationContext.gyldigSupplierOgConsumer(): Boolean {
    val supplier = this.getClaims("maskinporten").get("supplier") as Map<String, String>
    val consumer = this.getClaims("maskinporten").get("consumer") as Map<String, String>
    val supplierOrgnr = supplier.extractOrgnummer()
    val consumerOrgnr = consumer.extractOrgnummer()
    return supplierOrgnr != null &&
        consumerOrgnr != null &&
        supplierOrgnr.matches(Regex("\\d{9}")) &&
        consumerOrgnr.matches(Regex("\\d{9}"))
}

private fun Map<String, String>.extractOrgnummer(): String? =
    get("ID")
        ?.split(":")
        ?.get(1)
