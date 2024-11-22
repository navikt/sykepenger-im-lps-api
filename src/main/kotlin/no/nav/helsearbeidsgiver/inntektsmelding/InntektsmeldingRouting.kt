package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSupplierOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun Route.filtrerInntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    post("/inntektsmeldinger") {
        try {
            val params = call.receive<InntektsmeldingRequest>()
            val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
            val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
            sikkerLogger().info("Received request with params: $params")
            if (consumerOrgnr != null) {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$consumerOrgnr]")
                inntektsmeldingService
                    .hentInntektsMeldingByRequest(
                        orgnr = consumerOrgnr,
                        request = params,
                    ).takeIf { it.antallInntektsmeldinger > 0 }
                    ?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound, "Ingen inntektsmeldinger funnet")
            } else {
                sikkerLogger().warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
                call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
            }
        } catch (e: Exception) {
            sikkerLogger().error("Error while processing request: {}", e)
            call.respond(HttpStatusCode.InternalServerError, "Error while processing request" + e.message)
        }
    }
}

fun Route.inntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    get("/inntektsmeldinger") {
        try {
            val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
            val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
            if (consumerOrgnr != null) {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$consumerOrgnr]")
                inntektsmeldingService
                    .hentInntektsmeldingerByOrgNr(consumerOrgnr)
                    .takeIf { it.antallInntektsmeldinger > 0 }
                    ?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound, "Ingen inntektsmeldinger funnet")
            } else {
                sikkerLogger().warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
                call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
            }
        } catch (e: Exception) {
            sikkerLogger().error("Error while processing request: {}", e)
            call.respond(HttpStatusCode.InternalServerError, "Error while processing request" + e.message)
        }
    }
}
