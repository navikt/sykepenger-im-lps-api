package no.nav.helsearbeidsgiver.innsending

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun Route.innsendingV1(innsendingService: InnsendingService) {
    route("/v1") {
        innsending(innsendingService)
        nyInntektsmelding(innsendingService)
    }
}

private fun Route.innsending(innsendingService: InnsendingService) {
    // Send inn inntektsmelding
    post("/inntektsmelding") {
        try {
            val request = this.call.receive<SkjemaInntektsmelding>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            sikkerLogger().info("Mottatt innsending: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] sender inn skjema på vegne av bedrift: [$sluttbrukerOrgnr]")

            request.valider().takeIf { it.isNotEmpty() }?.let {
                call.respond(HttpStatusCode.BadRequest, it)
                return@post
            }

            val lagreInnsending = innsendingService.lagreInnsending(sluttbrukerOrgnr, lpsOrgnr, request)

            call.respond(HttpStatusCode.Created, lagreInnsending.toString())
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved lagring av innsending: {$e}", e)
            call.respond(HttpStatusCode.InternalServerError, "En feil oppstod")
        }
    }
}

private fun Route.nyInntektsmelding(innsendingService: InnsendingService) {
    /*
        Her kommer dokumentasjon - dette er finfint
        Slik sender du inn: bla bla bla
        Ikke glem: lorem ipsum
     */
    post("/ny_inntektsmelding") {
        try {
            val request = this.call.receive<Skjema>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            sikkerLogger().info("Mottatt innsending: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] sender inn skjema på vegne av bedrift: [$sluttbrukerOrgnr]")

            request.valider().takeIf { it.isNotEmpty() }?.let {
                call.respond(HttpStatusCode.BadRequest, it)
                return@post
            }
            val interntSkjema = request.tilSkjemaInntektsmelding()
            interntSkjema.valider().takeIf { it.isNotEmpty() }?.let {
                call.respond(HttpStatusCode.BadRequest, it)
                return@post
            }
            val lagreInnsending = innsendingService.lagreInnsending(sluttbrukerOrgnr, lpsOrgnr, interntSkjema)

            call.respond(HttpStatusCode.Created, lagreInnsending.toString())
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved lagring av innsending: {$e}", e)
            call.respond(HttpStatusCode.InternalServerError, "En feil oppstod")
        }
    }
}
