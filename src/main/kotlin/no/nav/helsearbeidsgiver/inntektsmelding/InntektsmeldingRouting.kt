package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun Route.filtrerInntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    post("/inntektsmeldinger") {
        try {
            val request = call.receive<InntektsmeldingRequest>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("Mottat request: $request")
            if (sluttbrukerOrgnr != null) {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$sluttbrukerOrgnr]")
                inntektsmeldingService
                    .hentInntektsMeldingByRequest(
                        orgnr = sluttbrukerOrgnr,
                        request = request,
                    ).takeIf { it.antallInntektsmeldinger > 0 }
                    ?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound, "Ingen inntektsmeldinger funnet")
            } else {
                sikkerLogger().warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
                call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
            }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}

fun Route.inntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    get("/inntektsmeldinger") {
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (sluttbrukerOrgnr != null) {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$sluttbrukerOrgnr]")
                inntektsmeldingService
                    .hentInntektsmeldingerByOrgNr(sluttbrukerOrgnr)
                    .takeIf { it.antallInntektsmeldinger > 0 }
                    ?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound, "Ingen inntektsmeldinger funnet")
            } else {
                sikkerLogger().warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
                call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
            }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}
