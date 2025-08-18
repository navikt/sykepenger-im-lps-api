package no.nav.helsearbeidsgiver.soeknad

import io.ktor.http.HttpStatusCode
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
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

private val SOKNAD_RESSURS = Env.getProperty("ALTINN_SOKNAD_RESSURS")

fun Route.soeknadV1(soeknadService: SoeknadService) {
    route("/v1") {
        soeknad(soeknadService)
        filtrerSoeknader(soeknadService)
        soeknader(soeknadService)
    }
}

private fun Route.soeknad(soeknadService: SoeknadService) {
    // Hent én sykepengesøknad basert på søknadId
    get("/sykepengesoeknad/{soeknadId}") {
        try {
            val soeknadId = call.parameters["soeknadId"]?.let { UUID.fromString(it) }
            requireNotNull(soeknadId) { "soeknadId: $soeknadId ikke gyldig UUID" }

            val soeknad = soeknadService.hentSoeknad(soeknadId)

            if (soeknad == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen søknad for id $soeknadId")
                return@get
            }
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = SOKNAD_RESSURS,
                    orgnumre = setOf(soeknad.arbeidsgiver.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soeknadId] på vegne av orgnr: $systembrukerOrgnr")

            call.respond(soeknad)
        } catch (e: IllegalArgumentException) {
            sikkerLogger().error(e.message, e)
            call.respond(HttpStatusCode.NotFound, "Ikke gyldig søknadId")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av søknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av søknad")
        }
    }
}

@Deprecated(
    message =
        "Fungerer kun dersom systembruker er satt opp på sluttbruker-organisasjonens underenhet. " +
            "Vi anbefaler å bruke POST /sykepengesoeknader istedenfor.",
    level = DeprecationLevel.WARNING,
)
private fun Route.soeknader(soeknadService: SoeknadService) {
    // Hent sykepengesøknader sendt til tilhørende systembrukers orgnr.
    get("/sykepengesoeknader") {
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknader for bedrift: [$sluttbrukerOrgnr]")
            if (!tokenValidationContext().harTilgangTilRessurs(SOKNAD_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            val soeknader: List<Sykepengesoeknad> =
                soeknadService.hentSoeknader(
                    sluttbrukerOrgnr,
                )
            call.respond(soeknader)
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av søknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av søknader")
        }
    }
}

private fun Route.filtrerSoeknader(soeknadService: SoeknadService) {
    // Filtrer søknader på orgnr (underenhet), fnr og/eller dato søknaden ble mottatt av NAV.
    post("/sykepengesoeknader") {
        try {
            val filter = call.receive<SykepengesoeknadFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = SOKNAD_RESSURS,
                    orgnumre = setOf(filter.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }

            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykepengesøknader for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            call.respond(soeknadService.hentSoeknader(filter = filter))
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig filterparameter")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykepengesøknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykepengesøknader")
        }
    }
}
