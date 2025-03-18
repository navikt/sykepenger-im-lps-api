package no.nav.helsearbeidsgiver.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.forespoersel.forespoerselV1
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldingV1

fun Application.configureRouting(services: Services) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate("systembruker-config") {
            inntektsmeldingV1(inntektsmeldingService = services.inntektsmeldingService, innsendingService = services.innsendingService)
            forespoerselV1(forespoerselService = services.forespoerselService)
        }
    }
}
