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

fun Route.soknadV1(soknadService: SoknadService) {
    route("/v1") {
        soknader(soknadService)
    }
}

private fun Route.soknader(soknadService: SoknadService) {
    // Hent forespørsler for tilhørende systembrukers orgnr.
    get("/soknader") {
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknader for bedrift: [$sluttbrukerOrgnr]")
            val soknader = soknadService.hentSoknader(sluttbrukerOrgnr)
            call.respond(soknader)
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av søknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av søknader")
        }
    }
}
