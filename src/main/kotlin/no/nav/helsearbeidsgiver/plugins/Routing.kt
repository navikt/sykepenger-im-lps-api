package no.nav.helsearbeidsgiver.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.forespoersel.filtererForespoersler
import no.nav.helsearbeidsgiver.forespoersel.forespoersler
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.inntektsmelding.filtrerInntektsmeldinger
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldinger

fun Application.configureRouting(
    forespoerselService: ForespoerselService,
    inntektsmeldingService: InntektsmeldingService,
) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate("systembruker-config") {
            filtrerInntektsmeldinger(inntektsmeldingService)
            inntektsmeldinger(inntektsmeldingService)
            forespoersler(forespoerselService)
            filtererForespoersler(forespoerselService)
        }
    }
}
