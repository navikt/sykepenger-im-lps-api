package no.nav.helsearbeidsgiver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import org.slf4j.LoggerFactory

private val forespoerselService = ForespoerselService()
private val inntektsmeldingService = InntektsmeldingService()
private val LOG = LoggerFactory.getLogger("applikasjonslogger")

fun Application.configureRouting() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("validToken") {
            get("/forespoersler") {
                val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
                if (consumerOrgnr != null) {
                    LOG.info("Henter forespørsler for orgnr: $consumerOrgnr")
                    call.respond(forespoerselService.hentForespoerslerForOrgnr(consumerOrgnr))
                } else {
                    LOG.warn("Consumer orgnr mangler")
                    call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
                }
            }
            get("/inntektsmeldinger") {
                val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()

                if (consumerOrgnr != null) {
                    LOG.info("Henter inntektsmeldinger for orgnr: $consumerOrgnr")
                    call.respond(inntektsmeldingService.hentInntektsmeldingerByOrgNr(consumerOrgnr))
                } else {
                    LOG.warn("Consumer orgnr mangler")
                    call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
                }
            }
        }
    }
}
