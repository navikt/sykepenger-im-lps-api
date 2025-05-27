package no.nav.helsearbeidsgiver.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.forespoersel.forespoerselV1
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldingV1
import no.nav.helsearbeidsgiver.soknad.soknadV1
import no.nav.helsearbeidsgiver.sykmelding.sykmeldingV1

fun Application.configureRouting(services: Services) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate("systembruker-config") {
            inntektsmeldingV1(
                services = services,
            )
            forespoerselV1(services = services)
            sykmeldingV1(services.sykmeldingService, services.pdpService)
            soknadV1(services = services)
        }
    }
}
