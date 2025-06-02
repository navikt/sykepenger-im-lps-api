package no.nav.helsearbeidsgiver.soknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

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

            val soknader: List<Sykepengesoeknad> = soeknadService.hentSoeknader(sluttbrukerOrgnr)
            call.respond(soknader)
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
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soeknadId] på vegene av orgnr: $sluttbrukerOrgnr")

            val soknad = soeknadService.hentSoeknad(soeknadId, sluttbrukerOrgnr)
            if (soknad == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen søknad for id $soeknadId")
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
