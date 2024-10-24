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
import no.nav.helsearbeidsgiver.auth.getSupplierOrgnr
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
                val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
                if (consumerOrgnr != null) {
                    LOG.info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$consumerOrgnr]")
                    call.respond(forespoerselService.hentForespoerslerForOrgnr(consumerOrgnr))
                } else {
                    LOG.warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
                    call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
                }
            }
            get("/inntektsmeldinger") {
                val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
                val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
                if (consumerOrgnr != null) {
                    LOG.info("LPS [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$consumerOrgnr]")
                    call.respond(inntektsmeldingService.hentInntektsmeldingerByOrgNr(consumerOrgnr))
                } else {
                    LOG.warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
                    call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
                }
            }
        }
    }
}
