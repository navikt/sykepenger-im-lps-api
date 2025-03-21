package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

fun Route.sykmeldingV1(sykmeldingService: SykmeldingService) {
    route("/v1") {
        hentSykmelding(sykmeldingService)
    }
}

fun String?.isValidUuid(): Boolean =
    try {
        this?.let {
            UUID.fromString(it)
            true
        } ?: false
    } catch (e: IllegalArgumentException) {
        false
    }

private fun Route.hentSykmelding(sykmeldingService: SykmeldingService) {
    get("/sykmelding/{id}") {
        try {
            val sykmeldingId = call.parameters["id"]?.runCatching(UUID::fromString)?.getOrNull()
            if (sykmeldingId == null) {
                call.respond(HttpStatusCode.BadRequest, "Mottok ikke en gylid sykmelding ID parameter")
                return@get
            }
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift: [$sluttbrukerOrgnr]")
            sykmeldingService
                .hentSykmelding(sykmeldingId, sluttbrukerOrgnr)
                ?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.NotFound, "Ingen sykmelding funnet")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykmelding: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykmelding")
        }
    }
}
