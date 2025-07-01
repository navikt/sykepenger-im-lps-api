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
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

fun Route.forespoerselV1(forespoerselService: ForespoerselService) {
    route("/v1") {
        forespoersler(forespoerselService)
        forespoersel(forespoerselService)
        filtrerForespoersler(forespoerselService)
    }
}

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

@Deprecated(
    message =
        "Fungerer kun dersom systembruker er satt opp på sluttbruker-organisasjonens underenhet. " +
            "Bruk POST /forespoersler istedenfor.",
    level = DeprecationLevel.WARNING,
)
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
}

private fun Route.forespoersel(forespoerselService: ForespoerselService) {
    // Hent forespørsel med navReferanseId.
    get("/forespoersel/{navReferanseId}") {
        try {
            val navReferanseId = call.parameters["navReferanseId"]?.let { UUID.fromString(it) }
            requireNotNull(navReferanseId) { "navReferanseId: $navReferanseId ikke gyldig UUID" }

            val forespoersel = forespoerselService.hentForespoersel(navReferanseId)
            if (forespoersel == null) {
                call.respond(HttpStatusCode.NotFound, "Forespørsel med navReferanseId: $navReferanseId ikke funnet.")
                return@get
            }

            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS, Orgnr(forespoersel.orgnr))) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }

            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsel med id $navReferanseId for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                    " og forespørselOrgnr: [${forespoersel.orgnr}]",
            )
            call.respond(forespoersel)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}

private fun Route.filtrerForespoersler(forespoerselService: ForespoerselService) {
    // Hent forespørsler for orgnr, filtrer basert på request.
    // filterparametre fom og tom refererer til opprettetTid (Tidspunktet forespørselen ble opprettet av Nav)
    post("/forespoersler") {
        try {
            val request = call.receive<ForespoerselRequest>()

            if (!tokenValidationContext().harTilgangTilRessurs(ressurs = IM_RESSURS, orgnr = Orgnr(request.orgnr))) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsler for bedrift med systembrukerOrgnr: [$systembrukerOrgnr] og requestOrgnr: [${request.orgnr}]",
            )
            call.respond(forespoerselService.filtrerForespoersler(request))
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}
