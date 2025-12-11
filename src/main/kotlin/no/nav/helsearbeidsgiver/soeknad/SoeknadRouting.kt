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
import no.nav.helsearbeidsgiver.metrikk.tellDokumenterHentet
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

private val SOEKNAD_RESSURS = Env.getProperty("ALTINN_SOEKNAD_RESSURS")

fun Route.soeknadV1(
    soeknadService: SoeknadService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/v1") {
        soeknad(soeknadService, unleashFeatureToggles)
        filtrerSoeknader(soeknadService, unleashFeatureToggles)
    }
}

private fun Route.soeknad(
    soeknadService: SoeknadService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Hent én sykepengesøknad basert på søknadId
    get("/sykepengesoeknad/{soeknadId}") {
        try {
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!unleashFeatureToggles.skalEksponereSykepengesoeknader(orgnr = Orgnr(lpsOrgnr))) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            val soeknadId = call.parameters["soeknadId"]?.toUuidOrNull()
            if (soeknadId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ugyldig soeknadId"))
                return@get
            }

            val soeknad = soeknadService.hentSoeknad(soeknadId)

            if (soeknad == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Fant ingen søknad for id $soeknadId"))
                return@get
            }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = SOEKNAD_RESSURS,
                    orgnr = soeknad.arbeidsgiver.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Ikke tilgang til ressurs"))
                return@get
            }
            tellApiRequest()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soeknadId] på vegne av orgnr: $systembrukerOrgnr")
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKEPENGESOEKNAD)

            call.respond(soeknad)
        } catch (e: Exception) {
            "Feil ved henting av sykepengesøknad".also {
                logger().error(it)
                sikkerLogger().error(it, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(it))
            }
        }
    }
}

private fun Route.filtrerSoeknader(
    soeknadService: SoeknadService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Filtrer søknader på orgnr (underenhet), fnr og/eller dato søknaden ble mottatt av NAV.

    post("/sykepengesoeknader") {
        try {
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!unleashFeatureToggles.skalEksponereSykepengesoeknader(orgnr = Orgnr(lpsOrgnr))) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }

            val filter = call.receive<SykepengesoeknadFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = SOEKNAD_RESSURS,
                    orgnr = filter.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Ikke tilgang til ressurs"))
                return@post
            }

            tellApiRequest()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykepengesøknader for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val soeknader = soeknadService.hentSoeknader(filter = filter)
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKEPENGESOEKNAD, soeknader.size)
            call.respondWithMaxLimit(soeknader)
            return@post
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ugyldig filterparameter"))
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Request mangler eller har ugyldig body"))
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykepengesøknader", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Feil ved henting av sykepengesøknader"))
        }
    }
}
