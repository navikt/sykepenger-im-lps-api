package no.nav.helsearbeidsgiver.soknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

private val SOKNAD_RESSURS = Env.getProperty("ALTINN_SOKNAD_RESSURS")

fun Route.soknadV1(services: Services) {
    route("/v1") {
        soknader(services)
    }
}

private fun Route.soknader(services: Services) {
    // Hent sykepengesøknader sendt til tilhørende systembrukers orgnr.
    get("/sykepengesoknader") {
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknader for bedrift: [$sluttbrukerOrgnr]")
            if (!tokenValidationContext().harTilgangTilRessurs(services.pdpService, SOKNAD_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            }
            val soknader: List<Sykepengesoknad> = services.soknadService.hentSoknader(sluttbrukerOrgnr)
            call.respond(soknader)
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av søknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av søknader")
        }
    }

    // Hent én sykepengesøknad basert på søknadId
    get("/sykepengesoknad/{soknadId}") {
        try {
            val soknadId = call.parameters["soknadId"]?.let { UUID.fromString(it) }
            requireNotNull(soknadId) { "soknadId: $soknadId ikke gyldig UUID" }

            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(services.pdpService, SOKNAD_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soknadId] på vegene av orgnr: $sluttbrukerOrgnr")

            val soknad = services.soknadService.hentSoknad(soknadId, sluttbrukerOrgnr)
            if (soknad == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen søknad for id $soknadId")
            } else {
                call.respond(soknad)
            }
        } catch (e: IllegalArgumentException) {
            sikkerLogger().error(e.message, e)
            call.respond(HttpStatusCode.NotFound, "Ikke gyldig søknadId")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av søknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av søknad")
        }
    }
}
