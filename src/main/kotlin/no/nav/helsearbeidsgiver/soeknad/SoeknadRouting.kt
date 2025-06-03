package no.nav.helsearbeidsgiver.soeknad

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
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

private val SOKNAD_RESSURS = Env.getProperty("ALTINN_SOKNAD_RESSURS")

fun Route.soeknadV1(soeknadService: SoeknadService) {
    route("/v1") {
        soeknader(soeknadService)
    }
}

private fun Route.soeknader(soeknadService: SoeknadService) {
    // Hent sykepengesøknader sendt til tilhørende systembrukers orgnr.
    get("/sykepengesoeknader") {
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknader for bedrift: [$sluttbrukerOrgnr]")
            if (!tokenValidationContext().harTilgangTilRessurs(SOKNAD_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            }
            val soeknader: List<Sykepengesoeknad> = soeknadService.hentSoeknader(sluttbrukerOrgnr)
            call.respond(soeknader)
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av søknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av søknader")
        }
    }

    // Hent én sykepengesøknad basert på søknadId
    get("/sykepengesoeknad/{soeknadId}") {
        try {
            val soeknadId = call.parameters["soeknadId"]?.let { UUID.fromString(it) }
            requireNotNull(soeknadId) { "soeknadId: $soeknadId ikke gyldig UUID" }

            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(SOKNAD_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soeknadId] på vegne av orgnr: $sluttbrukerOrgnr")

            val soeknad = soeknadService.hentSoeknad(soeknadId, sluttbrukerOrgnr)
            if (soeknad == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen søknad for id $soeknadId")
            } else {
                call.respond(soeknad)
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
