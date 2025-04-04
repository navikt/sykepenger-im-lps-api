package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.ApiFeil
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull

fun Route.sykmeldingV1(sykmeldingService: SykmeldingService) {
    route("/v1") {
        hentSykmelding(sykmeldingService)
    }
}

private fun Route.hentSykmelding(sykmeldingService: SykmeldingService) {
    get("/sykmelding/{id}") {
        try {
            // Lagt inn pga ktor3 - fjernet fangFeil() tmp fix
            val sykmeldingId = call.parameters["id"]?.toUuidOrNull() ?: throw ApiFeil(BadRequest, "Ugyldig sykmelding ID parameter")
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift: [$sluttbrukerOrgnr]")

            val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId, sluttbrukerOrgnr)

            sykmelding?.let { call.respond(it) } ?: throw ApiFeil(NotFound, "Ingen sykmelding funnet")
        } catch (e: ApiFeil) {
            sikkerLogger().error(e.feilMelding, e)
            call.respond(e.code, e.feilMelding)
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykmelding", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykmelding")
        }
    }
}
