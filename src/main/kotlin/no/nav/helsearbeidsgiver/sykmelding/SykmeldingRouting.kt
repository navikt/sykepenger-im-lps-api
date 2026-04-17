package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.serialization.JsonConvertException
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getPidFromTokenX
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.personHarTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.metrikk.MetrikkDokumentType
import no.nav.helsearbeidsgiver.metrikk.tellApiRequest
import no.nav.helsearbeidsgiver.metrikk.tellDokumenterHentet
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.Feil
import no.nav.helsearbeidsgiver.plugins.FeilMedReferanse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.plugins.serialiseringsErrorResponse
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.genererSykmeldingPdf
import no.nav.helsearbeidsgiver.utils.kapitaliserSykmeldtNavn
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.respondMedPDF
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
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        if (!unleashFeatureToggles.skalEksponereSykmeldinger(orgnr = Orgnr(lpsOrgnr))) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val sykmelding = hentSykmeldingMedId(sykmeldingService)
        if (sykmelding != null) {
            call.respond(sykmelding)
        }
    }
    // TODO: Fjern denne når den ikke lenger er nødvendig
    get("/sykmelding/{sykmeldingId}.pdf") {
        call.respondText(
            text = "Endepunktet har blitt flyttet. Bruk v1/sykmelding/{SYKMELDING_ID}/pdf i stedet.",
            status = HttpStatusCode.Gone,
        )
    }
    get("/sykmelding/{sykmeldingId}/pdf") {
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        if (!unleashFeatureToggles.skalEksponereSykmeldinger(Orgnr(lpsOrgnr))) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val sykmelding = hentSykmeldingMedId(sykmeldingService)
        if (sykmelding != null) {
            try {
                val pdfBytes = genererSykmeldingPdf(sykmelding.kapitaliserSykmeldtNavn())
                call.respondMedPDF(bytes = pdfBytes, filnavn = "sykmelding-${sykmelding.sykmeldingId}.pdf")
            } catch (e: Exception) {
                logger().error(Feil.FEIL_VED_PDF_GENERERING.feilmelding)
                sikkerLogger().error(Feil.FEIL_VED_PDF_GENERERING.feilmelding, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_PDF_GENERERING))
            }
        }
    }
}

private suspend fun RoutingContext.hentSykmeldingMedId(sykmeldingService: SykmeldingService): Sykmelding? {
    try {
        val sykmeldingId = call.parameters["sykmeldingId"]?.toUuidOrNull()
        if (sykmeldingId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_SYKMELDING_ID))
            return null
        }

        val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId)
        if (sykmelding == null) {
            call.respond(NotFound, ErrorResponse(FeilMedReferanse.SYKMELDING_IKKE_FUNNET, sykmeldingId))
            return null
        }

        if (!tokenValidationContext().harTilgangTilRessurs(
                ressurs = SM_RESSURS,
                orgnr = sykmelding.arbeidsgiver.orgnr.verdi,
            )
        ) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
            return null
        }
        tellApiRequest()
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
        val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
        sikkerLogger().info(
            "LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift med systembrukerOrgnr: " +
                "[$systembrukerOrgnr] og sykmeldingOrgnr: [${sykmelding.arbeidsgiver.orgnr}]",
        )
        tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKMELDING)

        return sykmelding
    } catch (e: Exception) {
        logger().error(Feil.FEIL_VED_HENTING_SYKMELDING.feilmelding)
        sikkerLogger().error(Feil.FEIL_VED_HENTING_SYKMELDING.feilmelding, e)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_SYKMELDING))
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
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
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
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_IDENTIFIKATOR))
        } catch (e: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, serialiseringsErrorResponse(e))
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_REQUEST_BODY))
        } catch (e: Exception) {
            sikkerLogger().error(Feil.FEIL_VED_HENTING_SYKMELDINGER.feilmelding, e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_SYKMELDINGER))
        }
    }
}

fun Route.sykmeldingTokenX(sykmeldingService: SykmeldingService) {
    route("/intern/personbruker") {
        get("/sykmelding/{sykmeldingId}/pdf") {
            try {
                val tokenContext = tokenValidationContext()
                val pid = tokenContext.getPidFromTokenX()

                if (pid == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.MANGLER_BRUKERIDENTIFIKASJON))
                    return@get
                }

                val sykmeldingId = call.parameters["sykmeldingId"]?.toUuidOrNull()
                if (sykmeldingId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_SYKMELDING_ID))
                    return@get
                }

                val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId)
                if (sykmelding == null) {
                    call.respond(NotFound, ErrorResponse(FeilMedReferanse.SYKMELDING_IKKE_FUNNET, sykmeldingId))
                    return@get
                }
                if (!tokenContext.personHarTilgangTilRessurs(
                        ressurs = SM_RESSURS,
                        orgnr = sykmelding.arbeidsgiver.orgnr.verdi,
                        pid = pid,
                    )
                ) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
                    return@get
                }
                // TODO: Legg til Prometheus metrikk telling
                sikkerLogger().info("Bruker med PID: $pid henter sykmelding PDF: $sykmeldingId")

                val pdfBytes = genererSykmeldingPdf(sykmelding.kapitaliserSykmeldtNavn())
                call.respondMedPDF(bytes = pdfBytes, filnavn = "sykmelding-${sykmelding.sykmeldingId}.pdf")
            } catch (e: Exception) {
                logger().error(Feil.FEIL_VED_HENTING_SYKMELDING.feilmelding)
                sikkerLogger().error(Feil.FEIL_VED_HENTING_SYKMELDING.feilmelding, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_SYKMELDING))
            }
        }
    }
}
