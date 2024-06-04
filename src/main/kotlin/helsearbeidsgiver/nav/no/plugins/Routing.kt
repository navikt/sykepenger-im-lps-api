package helsearbeidsgiver.nav.no.plugins

import helsearbeidsgiver.nav.no.forespoersel.ForespoerselService
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val forespoerselService = ForespoerselService()
fun Application.configureRouting() {

    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")
        get("/") {
            call.respondText("Hello World!")
        }
        get("/forespoersler") {
            call.respondText(forespoerselService .hentForespoersler().toString())
        }
    }
}
