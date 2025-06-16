package no.nav.helsearbeidsgiver.helsesjekker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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
            val result = database.connector().prepareStatement("SELECT 1 FROM dual", true)
            result.executeQuery()
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Could not connect to database :(")
        }
        call.respond(HttpStatusCode.OK, "I am Ready :)")
    }
}
