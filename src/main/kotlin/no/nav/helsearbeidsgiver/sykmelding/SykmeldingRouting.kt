package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
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
import no.nav.helsearbeidsgiver.utils.ApiFeil
import no.nav.helsearbeidsgiver.utils.fangFeil
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull

private val SM_RESSURS = Env.getProperty("ALTINN_SM_RESSURS")

fun Route.sykmeldingV1(sykmeldingService: SykmeldingService) {
    route("/v1") {
        sykmeldinger(sykmeldingService)
        sykmelding(sykmeldingService)
        filtrerSykmeldinger(sykmeldingService)
    }
}

private fun Route.sykmelding(sykmeldingService: SykmeldingService) {
    // Hent sykmelding med id
    // Orgnr i systembruker token må samsvare med orgnr i sykmeldingen
    get("/sykmelding/{id}") {
        fangFeil("Feil ved henting av sykmelding") {
            val sykmeldingId =
                call.parameters["id"]?.toUuidOrNull() ?: throw ApiFeil(BadRequest, "Ugyldig sykmelding ID parameter")
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(SM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                // kan ikke gjøre return@get her pga fangFeil-håndtering, men else-blokken slår uansett ikke til
            } else {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift: [$sluttbrukerOrgnr]")
                val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId, sluttbrukerOrgnr)
                sykmelding?.let { call.respond(it) } ?: throw ApiFeil(NotFound, "Ingen sykmelding funnet")
            }
        }
    }
}

@Deprecated(
    message =
        "Fungerer kun dersom systembruker er satt opp på sluttbruker-organisasjonens underenhet. " +
            "Vi anbefaler å bruke POST /sykmeldinger istedenfor.",
    level = DeprecationLevel.WARNING,
)
private fun Route.sykmeldinger(sykmeldingService: SykmeldingService) {
    get("/sykmeldinger") {
        // Hent alle sykmeldinger for et orgnr
        // Orgnr i systembruker token må samsvare med orgnr i sykmeldingen
        fangFeil("Feil ved henting av sykmeldinger") {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(SM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            } else {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter sykmeldinger for bedrift: [$sluttbrukerOrgnr]")
                val sykmeldinger = sykmeldingService.hentSykmeldinger(sluttbrukerOrgnr)
                call.respond(sykmeldinger)
            }
        }
    }
}

private fun Route.filtrerSykmeldinger(sykmeldingService: SykmeldingService) {
    // Filtrer sykmeldinger på fnr og / eller dato (mottattAvNav)
    post("/sykmeldinger") {
        // Hent alle sykmeldinger for et orgnr, filtrert med parametere
        // Orgnr i systembruker token må samsvare med orgnr i sykmeldingen
        val sykmeldingFilterRequest = call.receive<SykmeldingFilterRequest>()
        fangFeil("Feil ved henting av sykmeldinger") {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(SM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            } else {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter sykmeldinger for bedrift: [$sluttbrukerOrgnr]")
                val sykmeldinger = sykmeldingService.hentSykmeldinger(sluttbrukerOrgnr, sykmeldingFilterRequest)
                call.respond(sykmeldinger)
            }
        }
    }
}
