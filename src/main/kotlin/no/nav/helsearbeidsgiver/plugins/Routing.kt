package no.nav.helsearbeidsgiver.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.acceptItems
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.forespoersel.forespoerselV1
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldingV1
import no.nav.helsearbeidsgiver.soeknad.soeknadV1
import no.nav.helsearbeidsgiver.sykmelding.sykmeldingV1
import no.nav.helsearbeidsgiver.utils.registry


fun Application.configureRouting(services: Services) {
    routing {
        get("metrics") {
            call.request.acceptItems().firstOrNull()?.let {
                val contentType = ContentType.parse(it.value)
                val metrics = registry.scrape(it.value)

                call.respondText(metrics, contentType)
            } ?: call.respond(HttpStatusCode.NotAcceptable, "Supported types: application/openmetrics-text and text/plain")
        }
        naisRoutes(services.helseSjekkService)
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate("systembruker-config") {
            inntektsmeldingV1(
                services = services,
            )
            forespoerselV1(forespoerselService = services.forespoerselService)
            sykmeldingV1(sykmeldingService = services.sykmeldingService)
            soeknadV1(soeknadService = services.soeknadService)
        }
    }
}

suspend inline fun <reified T> ApplicationCall.respondWithMaxLimit(entities: List<T>) {
    if (entities.size > MAX_ANTALL_I_RESPONS) {
        response.header("X-Warning-limit-reached", MAX_ANTALL_I_RESPONS)
        val liste = entities.subList(0, MAX_ANTALL_I_RESPONS)
        respond(liste)
    } else {
        respond(entities)
    }
}
