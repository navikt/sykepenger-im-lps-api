package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.contentType
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.metrikk.MetrikkDokumentType
import no.nav.helsearbeidsgiver.metrikk.tellApiRequest
import no.nav.helsearbeidsgiver.metrikk.tellDokumenterHentet
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_SYKMELDING
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_SYKMELDINGER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.IKKE_TILGANG_TIL_RESSURS
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_FILTERPARAMETER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_IDENTIFIKATOR
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_REQUEST_BODY
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_SYKMELDING_ID
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.helsearbeidsgiver.utils.genererSykmeldingPdf
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.pipe.orDefault
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

private val SM_RESSURS = Env.getProperty("ALTINN_SM_RESSURS")

fun Route.sykmeldingV1(
    sykmeldingService: SykmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/v1") {
        sykmelding(sykmeldingService, unleashFeatureToggles)
        filtrerSykmeldinger(sykmeldingService, unleashFeatureToggles)
    }
}

private fun Route.sykmelding(
    sykmeldingService: SykmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    get("/sykmelding/{sykmeldingId}") {
        val sykmelding = hentSykmeldingMedId(unleashFeatureToggles, sykmeldingService)
        if (sykmelding != null) {
            call.respond(sykmelding)
        }
    }
    get("/sykmelding/{sykmeldingId}.pdf") {
        val sykmelding = hentSykmeldingMedId(unleashFeatureToggles, sykmeldingService)
        if (sykmelding != null) {
            val pdfBytes = genererSykmeldingPdf(sykmelding)
            call.response.header(HttpHeaders.ContentDisposition, "inline; filename=\"sykmelding-${sykmelding.sykmeldingId}.pdf\"")
            call.respondBytes(
                bytes = pdfBytes,
                contentType = ContentType.Application.Pdf,
                status = HttpStatusCode.OK,
            )
        }
    }
}


private suspend fun RoutingContext.hentSykmeldingMedId(
    unleashFeatureToggles: UnleashFeatureToggles,
    sykmeldingService: SykmeldingService
): Sykmelding? {
    try {
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()

        if (!unleashFeatureToggles.skalEksponereSykmeldinger(orgnr = Orgnr(lpsOrgnr))) {
            call.respond(HttpStatusCode.Forbidden)
            return null
        }

        val sykmeldingId = call.parameters["sykmeldingId"]?.toUuidOrNull()
        if (sykmeldingId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_SYKMELDING_ID))
            return null
        }

        val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId)
        if (sykmelding == null) {
            call.respond(NotFound, ErrorResponse("Sykmelding med id: $sykmeldingId ikke funnet."))
            return null
        }

        if (!tokenValidationContext().harTilgangTilRessurs(
                ressurs = SM_RESSURS,
                orgnr = sykmelding.arbeidsgiver.orgnr.verdi,
            )
        ) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
            return null
        }
        tellApiRequest()
        sikkerLogger().info(
            "LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                    " og sykmeldingOrgnr: [${sykmelding.arbeidsgiver.orgnr}]",
        )
        tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKMELDING)

        return sykmelding
    } catch (e: Exception) {
        FEIL_VED_HENTING_SYKMELDING.also {
            logger().error(it)
            sikkerLogger().error(it, e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(it))
        }
    }
    return null
}

private fun Route.filtrerSykmeldinger(
    sykmeldingService: SykmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Filtrer sykmeldinger på orgnr (underenhet), fnr og/eller dato sykmeldingen ble mottatt av NAV.
    post("/sykmeldinger") {
        try {
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!unleashFeatureToggles.skalEksponereSykmeldinger(orgnr = Orgnr(lpsOrgnr))) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }

            val filter = call.receive<SykmeldingFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = SM_RESSURS,
                    orgnr = filter.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
                return@post
            }

            tellApiRequest()
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykmeldinger for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val sykemeldinger = sykmeldingService.hentSykmeldinger(filter)

            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKMELDING, antall = sykemeldinger.size)
            call.respondWithMaxLimit(sykemeldinger)
            return@post
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_IDENTIFIKATOR))
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_FILTERPARAMETER))
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_REQUEST_BODY))
        } catch (e: Exception) {
            sikkerLogger().error(FEIL_VED_HENTING_SYKMELDINGER, e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(FEIL_VED_HENTING_SYKMELDINGER))
        }
    }
}
