package no.nav.helsearbeidsgiver.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.forespoersel.forespoerselV1
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldingV1
import no.nav.helsearbeidsgiver.soeknad.soeknadV1
import no.nav.helsearbeidsgiver.sykmelding.sykmeldingV1

fun Application.configureRouting(services: Services) {
    routing {
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

suspend inline fun <reified T> respondWithMaxLimit(
    call: ApplicationCall,
    entities: List<T>,
) {
    if (entities.size > MAX_ANTALL_I_RESPONS) {
        call.response.header("X-Warning-limit-reached", MAX_ANTALL_I_RESPONS)
        val liste = entities.subList(0, MAX_ANTALL_I_RESPONS)
        call.respond(liste)
    } else {
        call.respond(entities)
    }
}
