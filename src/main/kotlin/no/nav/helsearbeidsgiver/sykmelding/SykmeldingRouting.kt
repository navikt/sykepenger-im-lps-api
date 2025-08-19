package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.plugins.BadRequestException
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
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

private val SM_RESSURS = Env.getProperty("ALTINN_SM_RESSURS")

fun Route.sykmeldingV1(sykmeldingService: SykmeldingService) {
    route("/v1") {
        sykmeldinger(sykmeldingService)
        sykmelding(sykmeldingService)
        filtrerSykmeldinger(sykmeldingService)
    }
}

private fun Route.sykmelding(sykmeldingService: SykmeldingService) {
    // Hent sykmelding med sykmeldingId
    get("/sykmelding/{sykmeldingId}") {
        try {
            val sykmeldingId = call.parameters["sykmeldingId"]?.toUuidOrNull()
            requireNotNull(sykmeldingId) { "navReferanseId: $sykmeldingId ikke gyldig UUID" }

            val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId)
            if (sykmelding == null) {
                call.respond(NotFound, "Sykmelding med id: $sykmeldingId ikke funnet.")
                return@get
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = SM_RESSURS,
                    orgnumre = setOf(sykmelding.arbeidsgiver.orgnr.toString(), systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                    " og sykmeldingOrgnr: [${sykmelding.arbeidsgiver.orgnr}]",
            )
            call.respond(sykmelding)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykmelding", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykmelding")
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
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(SM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
            } else {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter sykmeldinger for bedrift: [$sluttbrukerOrgnr]")
                val sykmeldinger = sykmeldingService.hentSykmeldinger(sluttbrukerOrgnr)
                call.respond(sykmeldinger)
            }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykmeldinger", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykmeldinger")
        }
    }
}

private fun Route.filtrerSykmeldinger(sykmeldingService: SykmeldingService) {
    // Filtrer sykmeldinger på orgnr (underenhet), fnr og/eller dato sykmeldingen ble mottatt av NAV.
    post("/sykmeldinger") {
        try {
            val filter = call.receive<SykmeldingFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = SM_RESSURS,
                    orgnumre = setOf(filter.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }

            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykmeldinger for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            respondWithMaxLimit(call, sykmeldingService.hentSykmeldinger(filter))
            return@post
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig filterparameter")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykmeldinger", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykmeldinger")
        }
    }
}
