package no.nav.helsearbeidsgiver.helsesjekker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.naisRoutes(helseSjekkService: HelseSjekkService) {
    route("/health") {
        isAlive()
        isReady(helseSjekkService)
    }
}

fun Route.isAlive() {
    get("/is-alive") {
        call.respond(HttpStatusCode.OK, "Alive")
    }
}

fun Route.isReady(helseSjekkService: HelseSjekkService) {
    get("/is-ready") {
        if (helseSjekkService.isReady()) {
            call.respond(HttpStatusCode.OK, "Ready")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Not Ready")
        }
    }
}
