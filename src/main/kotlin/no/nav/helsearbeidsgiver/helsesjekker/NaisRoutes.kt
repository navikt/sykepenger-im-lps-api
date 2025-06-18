package no.nav.helsearbeidsgiver.helsesjekker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.utils.log.logger

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
        logger().debug("is-ready -- Start")
        if (helseSjekkService.databaseIsAlive()) {
            logger().debug("is-ready -- Ready!")
            call.respond(HttpStatusCode.OK, "Ready")
        } else {
            logger().debug("is-ready -- Feil")
            call.respond(HttpStatusCode.InternalServerError, "Could not connect to database :(")
        }
        logger().debug("is-ready -- End")
    }
}
