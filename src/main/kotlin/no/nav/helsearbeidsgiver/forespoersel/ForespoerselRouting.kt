package no.nav.helsearbeidsgiver.forespoersel

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

fun Route.forespoerselV1(forespoerselService: ForespoerselService) {
    route("/v1") {
        forespoersler(forespoerselService)
        filtererForespoersler(forespoerselService)
    }
}

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

private fun Route.forespoersler(forespoerselService: ForespoerselService) {
    // Hent forespørsler for tilhørende systembrukers orgnr.
    get("/forespoersler") {
        try {
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$sluttbrukerOrgnr]")
            call.respond(forespoerselService.hentForespoerslerForOrgnr(sluttbrukerOrgnr))
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }

    get("/forespoersel/{navReferanseId}") {
        try {
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            val navReferanseId = call.parameters["navReferanseId"]?.let { UUID.fromString(it) }
            requireNotNull(navReferanseId) { "navReferanseId: $navReferanseId ikke gyldig UUID" }
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter forespørsel for bedrift: [$sluttbrukerOrgnr]")
            val fsp = forespoerselService.hentForespoersel(navReferanseId, sluttbrukerOrgnr)
            if (fsp != null) {
                call.respond(fsp)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}

private fun Route.filtererForespoersler(forespoerselService: ForespoerselService) {
    // Hent forespørsler for tilhørende systembrukers orgnr, filtrer basert på request.
    post("/forespoersler") {
        try {
            val request = call.receive<ForespoerselRequest>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }
            sikkerLogger().info("Mottat request: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$sluttbrukerOrgnr]")
            call.respond(forespoerselService.filtrerForespoerslerForOrgnr(sluttbrukerOrgnr, request))
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}
