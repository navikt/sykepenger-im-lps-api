package helsearbeidsgiver.nav.no.plugins

import helsearbeidsgiver.nav.no.forespoersel.ForespoerselService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

private val forespoerselService = ForespoerselService()

private val LOG = LoggerFactory.getLogger("applikasjonslogger")

fun Application.configureRouting() {

    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("validToken")  {
            get("/forespoersler") {
                val supplier = call.getClaim("maskinporten", "supplier").extractOrgnummer()
                val consumer = call.getClaim("maskinporten", "consumer").extractOrgnummer()
                if (supplier.isNullOrEmpty() || consumer.isNullOrEmpty()) {
                    LOG.warn("Token er gyldig, men orgnummer er feil formattert eller mangler! supplier = $supplier, consumer=$consumer")
                    call.respond(HttpStatusCode.Forbidden)
                }
                LOG.info("$supplier har logget inn - representerer $consumer")
                call.respond(forespoerselService.hentForespoersler())
            }
        }
    }
}



private fun ApplicationCall.getClaim(issuer: String, field: String) : Map<String,String> =

        authentication.principal<TokenValidationContextPrincipal>()
            ?.context?.getClaims(issuer)?.get(field) as Map<String, String>


private fun Map<String, String>.extractOrgnummer() : String? = get("ID")
    ?.split(":")
    ?.get(1)
