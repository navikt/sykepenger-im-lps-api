package no.nav.helsearbeidsgiver.soeknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
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
import no.nav.helsearbeidsgiver.metrikk.MetrikkDokumentType
import no.nav.helsearbeidsgiver.metrikk.tellApiRequest
import no.nav.helsearbeidsgiver.metrikk.tellDokumentHentetMedMaxAntall
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

private val SOKNAD_RESSURS = Env.getProperty("ALTINN_SOKNAD_RESSURS")

fun Route.soeknadV1(soeknadService: SoeknadService) {
    route("/v1") {
        soeknad(soeknadService)
        filtrerSoeknader(soeknadService)
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
            tellApiRequest()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soeknadId] på vegne av orgnr: $systembrukerOrgnr")
            tellDokumentHentetMedMaxAntall(lpsOrgnr, MetrikkDokumentType.SYKEPENGESOEKNAD)
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
            tellApiRequest()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykepengesøknader for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val soeknader = soeknadService.hentSoeknader(filter = filter)
            tellDokumentHentetMedMaxAntall(lpsOrgnr, MetrikkDokumentType.SYKEPENGESOEKNAD, soeknader.size)
            call.respondWithMaxLimit(soeknader)
            return@post
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig filterparameter")
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, "Request mangler eller har ugyldig body")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykepengesøknader", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykepengesøknader")
        }
    }
}
