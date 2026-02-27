package no.nav.helsearbeidsgiver.soeknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
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
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_SYKEPENGESOEKNAD
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_SYKEPENGESOEKNADER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.IKKE_TILGANG_TIL_RESSURS
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_FILTERPARAMETER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_REQUEST_BODY
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_SOEKNAD_ID
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.genererSoeknadPdf
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.respondMedPDF
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
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        if (!unleashFeatureToggles.skalEksponereSykepengesoeknader(orgnr = Orgnr(lpsOrgnr))) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val soeknad = hentSoeknadMedId(soeknadService)
        if (soeknad != null) {
            call.respond(soeknad)
        }
    }

    get("/sykepengesoeknad/{soeknadId}/pdf") {
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        if (!unleashFeatureToggles.skalEksponereSykepengesoeknader(orgnr = Orgnr(lpsOrgnr)) ||
            !unleashFeatureToggles.skalEksponereSykepengesoeknaderPDF()
        ) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val soeknad = hentSoeknadMedId(soeknadService)
        if (soeknad != null) {
            // TODO: proof of concept implementasjon, forberdring muligheter her for henting av navn
            val soeknadMedNavn = soeknadService.tilSoeknadMedNavn(soeknad)
            val pdfBytes = genererSoeknadPdf(soeknadMedNavn)
            call.respondMedPDF(bytes = pdfBytes, filnavn = "sykepengesoeknad-${soeknad.soeknadId}.pdf")
        }
    }
}



private suspend fun RoutingContext.hentSoeknadMedId(soeknadService: SoeknadService): Sykepengesoeknad? {
    try {
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()

        val soeknadId = call.parameters["soeknadId"]?.toUuidOrNull()
        if (soeknadId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_SOEKNAD_ID))
            return null
        }

        val soeknad = soeknadService.hentSoeknad(soeknadId)
        if (soeknad == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Fant ingen søknad for id $soeknadId"))
            return null
        }

        if (!tokenValidationContext().harTilgangTilRessurs(
                ressurs = SOEKNAD_RESSURS,
                orgnr = soeknad.arbeidsgiver.orgnr,
            )
        ) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
            return null
        }

        tellApiRequest()
        sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soeknadId] på vegne av orgnr: $systembrukerOrgnr")
        tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKEPENGESOEKNAD)

        return soeknad
    } catch (e: Exception) {
        FEIL_VED_HENTING_SYKEPENGESOEKNAD.also {
            logger().error(it)
            sikkerLogger().error(it, e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(it))
        }
    }
    return null
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
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
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
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_FILTERPARAMETER))
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_REQUEST_BODY))
        } catch (e: Exception) {
            sikkerLogger().error(FEIL_VED_HENTING_SYKEPENGESOEKNADER, e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(FEIL_VED_HENTING_SYKEPENGESOEKNADER))
        }
    }
}
