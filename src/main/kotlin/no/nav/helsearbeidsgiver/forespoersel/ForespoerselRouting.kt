package no.nav.helsearbeidsgiver.forespoersel

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

fun Route.forespoersler(forespoerselService: ForespoerselService) {
    // Hent forespørsler for tilhørende systembrukers orgnr.
    get("/forespoersler") {
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$sluttbrukerOrgnr]")
            call.respond(forespoerselService.hentForespoerslerForOrgnr(sluttbrukerOrgnr))
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}

fun Route.filtererForespoersler(forespoerselService: ForespoerselService) {
    // Hent forespørsler for tilhørende systembrukers orgnr, filtrer basert på request.
    post("/forespoersler") {
        try {
            val request = call.receive<ForespoerselRequest>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("Mottat request: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$sluttbrukerOrgnr]")
            call.respond(forespoerselService.filtrerForespoerslerForOrgnr(sluttbrukerOrgnr, request))
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}
