package no.nav.helsearbeidsgiver.soeknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
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
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.Feil
import no.nav.helsearbeidsgiver.plugins.FeilMedReferanse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.plugins.serialiseringsfeilResponse
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
    get("/sykepengesoeknad/{soeknadId}") {
        val soeknad = hentSoeknadMedId(soeknadService, unleashFeatureToggles)
        if (soeknad != null) {
            call.respond(soeknad)
        }
    }

    get("/sykepengesoeknad/{soeknadId}/pdf") {
        val soeknad = hentSoeknadMedId(soeknadService, unleashFeatureToggles)
        if (soeknad != null) {
            try {
                val soeknadForPDF = soeknadService.tilSoeknadForPdf(soeknad)
                val pdfBytes = genererSoeknadPdf(soeknadForPDF)
                call.respondMedPDF(bytes = pdfBytes, filnavn = "sykepengesoeknad-${soeknad.soeknadId}.pdf")
            } catch (e: Exception) {
                logger().error(Feil.FEIL_VED_PDF_GENERERING.feilmelding)
                sikkerLogger().error(Feil.FEIL_VED_PDF_GENERERING.feilmelding, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_PDF_GENERERING))
            }
        }
    }
}

private suspend fun RoutingContext.hentSoeknadMedId(
    soeknadService: SoeknadService,
    unleashFeatureToggles: UnleashFeatureToggles,
): Sykepengesoeknad? {
    try {
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        if (!unleashFeatureToggles.skalEksponereSykepengesoeknader(orgnr = Orgnr(lpsOrgnr))) {
            call.respond(HttpStatusCode.Forbidden)
            return null
        }
        val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()

        val soeknadId = call.parameters["soeknadId"]?.toUuidOrNull()
        if (soeknadId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_SOEKNAD_ID))
            return null
        }

        val soeknad = soeknadService.hentSoeknad(soeknadId)
        if (soeknad == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse(FeilMedReferanse.SOEKNAD_IKKE_FUNNET, soeknadId))
            return null
        }

        if (!tokenValidationContext().harTilgangTilRessurs(
                ressurs = SOEKNAD_RESSURS,
                orgnr = soeknad.arbeidsgiver.orgnr,
            )
        ) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
            return null
        }

        tellApiRequest()
        sikkerLogger().info("LPS: [$lpsOrgnr] henter søknad med id: [$soeknadId] på vegne av orgnr: $systembrukerOrgnr")
        tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKEPENGESOEKNAD)

        return soeknad
    } catch (e: Exception) {
        logger().error(Feil.FEIL_VED_HENTING_SYKEPENGESOEKNAD.feilmelding)
        sikkerLogger().error(Feil.FEIL_VED_HENTING_SYKEPENGESOEKNAD.feilmelding, e)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_SYKEPENGESOEKNAD))
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
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
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
        } catch (e: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, serialiseringsfeilResponse(e))
        } catch (e: Exception) {
            sikkerLogger().error(Feil.FEIL_VED_HENTING_SYKEPENGESOEKNADER.feilmelding, e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_SYKEPENGESOEKNADER))
        }
    }
}
