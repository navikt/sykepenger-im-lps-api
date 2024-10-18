package no.nav.helsearbeidsgiver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.getConsumerOrgnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

private val forespoerselService = ForespoerselService()
private val inntektsmeldingService = InntektsmeldingService()
private val LOG = LoggerFactory.getLogger("applikasjonslogger")

fun Application.configureRouting() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("validToken") {
            get("/forespoersler") {
                call.respond(forespoerselService.hentForespoersler())
            }
            get("/inntektsmeldinger") {
                val principal = call.principal<TokenValidationContextPrincipal>()
                val tokenValidationContext = principal?.context
                if (tokenValidationContext != null) {
                    val consumerOrgnr = tokenValidationContext.getConsumerOrgnr()
                    consumerOrgnr?.let {
                        LOG.info("Consumer orgnr: $it")

                        call.respond(inntektsmeldingService.hentInntektsmeldingerByOrgNr(consumerOrgnr))
                    }
                } else {
                    call.respondText("TokenValidationContext not found", status = HttpStatusCode.Unauthorized)
                }
            }
        }
        get("/imer") {
//            call.respond(inntektsmeldingService.hentInntektsmeldinger())
        }
    }
}
