package no.nav.helsearbeidsgiver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSupplierOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.utils.log.logger
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("applikasjonslogger")

fun Application.configureRouting(
    forespoerselService: ForespoerselService,
    inntektsmeldingService: InntektsmeldingService,
) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")
        filtrerInntektsmeldinger(inntektsmeldingService)
        authenticate("validToken") {
            forespoersler(forespoerselService)
            inntektsmeldinger(inntektsmeldingService)
        }
    }
}

private fun Routing.filtrerInntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    post("/inntektsmeldinger") {
        val params = call.receive<InntektsmeldingRequest>()
        logger().info("Received request with params: $params")
        inntektsmeldingService
            .hentInntektsMeldingByRequest(
                orgnr = "810007842",
                request = params,
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
    }
}

private fun Route.forespoersler(forespoerselService: ForespoerselService) {
    get("/forespoersler") {
        val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
        val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
        if (consumerOrgnr != null) {
            LOG.info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$consumerOrgnr]")
            call.respond(forespoerselService.hentForespoerslerForOrgnr(consumerOrgnr))
        } else {
            LOG.warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
            call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
        }
    }
}

private fun Route.inntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    get("/inntektsmeldinger") {
        val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
        val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
        if (consumerOrgnr != null) {
            LOG.info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$consumerOrgnr]")
            call.respond(inntektsmeldingService.hentInntektsmeldingerByOrgNr(consumerOrgnr))
        } else {
            LOG.warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
            call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
        }
    }
}
