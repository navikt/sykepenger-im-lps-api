package no.nav.helsearbeidsgiver.metrikk

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.acceptItems
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.metrikkRoutes() {
    get("metrics") {
        call.request.acceptItems().firstOrNull()?.let {
            val contentType = ContentType.parse(it.value)
            val metrics = registry.scrape(it.value)

            call.respondText(metrics, contentType)
        } ?: call.respond(HttpStatusCode.NotAcceptable, "Supported types: application/openmetrics-text and text/plain")
    }
}
