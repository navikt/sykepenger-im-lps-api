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
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

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
            "Vi anbefaler å bruke POST /forespoersler istedenfor.",
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
            val navReferanseId = call.parameters["navReferanseId"]?.toUuidOrNull()
            requireNotNull(navReferanseId) { "navReferanseId: $navReferanseId ikke gyldig UUID" }

            val forespoersel = forespoerselService.hentForespoersel(navReferanseId)
            if (forespoersel == null) {
                call.respond(HttpStatusCode.NotFound, "Forespørsel med navReferanseId: $navReferanseId ikke funnet.")
                return@get
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnumre = setOf(forespoersel.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsel med id $navReferanseId for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                    " og forespørselOrgnr: [${forespoersel.orgnr}]",
            )
            call.respond(forespoersel)
        } catch (_: IllegalArgumentException) {
            // TODO: Kan hende at vi må catche denne kun rundt navReferanse-parsing - gjelder andre entiteter /ruter også.
            // Eller vi kan pakke inn feil i egne service-exceptions el.l.
            // I søknad-rute boblet en require fra entitet-laget opp hit, og ga en uforståelig badrequest-feilmelding (og ingen logger)
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}

private fun Route.filtrerForespoersler(forespoerselService: ForespoerselService) {
    // Filtrer forespørsler om inntektsmelding på fnr, navReferanseId, status og / eller dato forespørselen ble opprettet av NAV.
    post("/forespoersler") {
        try {
            val request = call.receive<ForespoerselFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnumre = setOf(request.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }

            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsler for orgnr [${request.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
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
