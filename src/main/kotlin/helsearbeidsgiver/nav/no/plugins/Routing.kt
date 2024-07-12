package helsearbeidsgiver.nav.no.plugins

import helsearbeidsgiver.nav.no.forespoersel.ForespoerselService
import helsearbeidsgiver.nav.no.inntektsmelding.ImService
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val forespoerselService = ForespoerselService()
private val imService = ImService()

private val LOG = LoggerFactory.getLogger("applikasjonslogger")

fun Application.configureRouting() {

    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("validToken")  {
            get("/forespoersler") {
                call.respond(forespoerselService.hentForespoersler())
            }
            get("/inntektsmeldinger") {
                call.respond(imService.hentInntektsmeldinger())
            }
        }
        get("/imer") {
            call.respond(imService.hentInntektsmeldinger())
        }
    }
}




