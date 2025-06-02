package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.pdp.IPdpService
import no.nav.helsearbeidsgiver.utils.ApiFeil
import no.nav.helsearbeidsgiver.utils.fangFeil
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull

private val SM_RESSURS = Env.getProperty("ALTINN_SM_RESSURS")

// TODO: gjøre pdp-oppslag per route
fun Route.sykmeldingV1(
    sykmeldingService: SykmeldingService,
    pdpService: IPdpService,
) {
    route("/v1") {
        hentSykmelding(sykmeldingService, pdpService)
    }
}

private fun Route.hentSykmelding(
    sykmeldingService: SykmeldingService,
    pdpService: IPdpService,
) {
    // Hent sykmelding med id
    // Orgnr i systembruker token må samsvare med orgnr i sykmeldingen
    get("/sykmelding/{id}") {
        fangFeil("Feil ved henting av sykmelding") {
            val sykmeldingId = call.parameters["id"]?.toUuidOrNull() ?: throw ApiFeil(BadRequest, "Ugyldig sykmelding ID parameter")
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(pdpService, SM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            } else {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift: [$sluttbrukerOrgnr]")
                val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId, sluttbrukerOrgnr)
                sykmelding?.let { call.respond(it) } ?: throw ApiFeil(NotFound, "Ingen sykmelding funnet")
            }
        }
    }
}
