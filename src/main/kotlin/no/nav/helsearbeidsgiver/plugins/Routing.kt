package no.nav.helsearbeidsgiver.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.forespoersel.forespoerselV1
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.innsending.innsendingV1
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldingV1

fun Application.configureRouting(
    forespoerselService: ForespoerselService,
    inntektsmeldingService: InntektsmeldingService,
    innsendingService: InnsendingService,
) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate("systembruker-config") {
            inntektsmeldingV1(inntektsmeldingService)
            forespoerselV1(forespoerselService)
            innsendingV1(innsendingService)
        }
    }
}
