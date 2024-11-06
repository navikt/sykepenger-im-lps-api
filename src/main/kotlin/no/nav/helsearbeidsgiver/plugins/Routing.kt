package no.nav.helsearbeidsgiver.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.forespoersel.forespoersler
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.inntektsmelding.filtrerInntektsmeldinger
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldinger
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("applikasjonslogger")

fun Application.configureRouting(
    forespoerselService: ForespoerselService,
    inntektsmeldingService: InntektsmeldingService,
) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")

        authenticate("validToken") {
            filtrerInntektsmeldinger(inntektsmeldingService)
            forespoersler(forespoerselService)
            inntektsmeldinger(inntektsmeldingService)
        }
    }
}
