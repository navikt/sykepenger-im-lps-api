package no.nav.helsearbeidsgiver.helsesjekker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database

fun Route.naisRoutes(database: Database) {
    route("/health") {
        isAlive()
        isReady(database)
    }
}

fun Route.isAlive() {
    get("/is-alive") {
        call.respond(HttpStatusCode.OK, "I am Alive :)")
    }
}

fun Route.isReady(database: Database) {
    get("/is-ready") {
        try {
            logger().debug("is-ready -- Start")
            val result = database.connector().prepareStatement("SELECT 1", true)
            logger().debug("is-ready -- execute query")
            result.executeQuery()
            logger().debug("is-ready -- End")
        } catch (e: Exception) {
            logger().error("is-ready -- Exception: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, "Could not connect to database :(")
        }
        logger().info("is-ready -- Success")
        call.respond(HttpStatusCode.OK, "I am Ready :)")
    }
}
