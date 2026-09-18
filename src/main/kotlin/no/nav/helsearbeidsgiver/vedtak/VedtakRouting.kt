package no.nav.helsearbeidsgiver.vedtak

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
import no.nav.helsearbeidsgiver.auth.getPidFromTokenX
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.personHarTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.Feil
import no.nav.helsearbeidsgiver.plugins.FeilMedReferanse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.plugins.serialiseringsErrorResponse
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.genererVedtakPdf
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.respondMedPDF
import no.nav.helsearbeidsgiver.utils.toUuidOrNull

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

fun Route.vedtakV1(
    vedtakService: VedtakService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/v1") {
        get("/vedtak/{vedtakId}") {
            hentVedtakMedId(vedtakService, unleashFeatureToggles)?.let { call.respond(it) }
        }

        get("/vedtak/{vedtakId}/pdf") {
            val vedtak = hentVedtakMedId(vedtakService, unleashFeatureToggles)
            if (vedtak != null) {
                try {
                    val pdfBytes = genererVedtakPdf(vedtak)
                    call.respondMedPDF(bytes = pdfBytes, filnavn = "vedtak-${vedtak.vedtakId}.pdf")
                } catch (e: Exception) {
                    logger().error(Feil.FEIL_VED_PDF_GENERERING.feilmelding)
                    sikkerLogger().error(Feil.FEIL_VED_PDF_GENERERING.feilmelding, e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_PDF_GENERERING))
                }
            }
        }

        post("/vedtak") {
            try {
                if (!unleashFeatureToggles.skalEksponereVedtak()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }

                val tokenContext = tokenValidationContext()
                val lpsOrgnr = tokenContext.getConsumerOrgnr()
                val systembrukerOrgnr = tokenContext.getSystembrukerOrgnr()
                val filter = call.receive<VedtakFilter>()

                if (!tokenContext.harTilgangTilRessurs(
                        ressurs = IM_RESSURS,
                        orgnr = filter.orgnr,
                    )
                ) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
                    return@post
                }

                val vedtak = vedtakService.hentVedtak(filter)
                sikkerLogger().info(
                    "LPS: [$lpsOrgnr] henter vedtak for orgnr [${filter.orgnr}] " +
                        "på vegne av orgnr: $systembrukerOrgnr",
                )
                call.respondWithMaxLimit(vedtak)
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, serialiseringsErrorResponse(e))
            } catch (_: ContentTransformationException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_REQUEST_BODY))
            } catch (e: Exception) {
                logger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding)
                sikkerLogger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_VEDTAK))
            }
        }
    }
}

private suspend fun RoutingContext.hentVedtakMedId(
    vedtakService: VedtakService,
    unleashFeatureToggles: UnleashFeatureToggles,
): VedtakResponse? {
    try {
        if (!unleashFeatureToggles.skalEksponereVedtak()) {
            call.respond(HttpStatusCode.Forbidden)
            return null
        }

        val tokenContext = tokenValidationContext()
        val lpsOrgnr = tokenContext.getConsumerOrgnr()
        val systembrukerOrgnr = tokenContext.getSystembrukerOrgnr()
        val vedtakId = call.parameters["vedtakId"]?.toUuidOrNull()
        if (vedtakId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_VEDTAK_ID))
            return null
        }

        val vedtak = vedtakService.hentVedtak(vedtakId)
        if (vedtak == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse(FeilMedReferanse.VEDTAK_IKKE_FUNNET, vedtakId))
            return null
        }

        if (!tokenContext.harTilgangTilRessurs(
                ressurs = IM_RESSURS,
                orgnr = vedtak.orgnr,
            )
        ) {
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
            return null
        }

        sikkerLogger().info(
            "LPS: [$lpsOrgnr] henter vedtak med id: [$vedtakId] på vegne av orgnr: $systembrukerOrgnr",
        )
        return vedtak
    } catch (e: Exception) {
        logger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding)
        sikkerLogger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding, e)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_VEDTAK))
        return null
    }
}

fun Route.vedtakTokenX(
    vedtakService: VedtakService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/intern/personbruker") {
        get("/vedtak/{vedtakId}/pdf") {
            try {
                if (!unleashFeatureToggles.skalEksponereVedtak()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }

                val tokenContext = tokenValidationContext()
                val pid = tokenContext.getPidFromTokenX()

                if (pid == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.MANGLER_BRUKERIDENTIFIKASJON))
                    return@get
                }

                val vedtakId = call.parameters["vedtakId"]?.toUuidOrNull()
                if (vedtakId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_VEDTAK_ID))
                    return@get
                }

                val vedtak = vedtakService.hentVedtak(vedtakId)
                if (vedtak == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse(FeilMedReferanse.VEDTAK_IKKE_FUNNET, vedtakId),
                    )
                    return@get
                }

                if (!tokenContext.personHarTilgangTilRessurs(
                        ressurs = IM_RESSURS,
                        orgnr = vedtak.orgnr,
                        pid = pid,
                    )
                ) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
                    return@get
                }

                sikkerLogger().info("Bruker med PID: $pid henter vedtak PDF: $vedtakId")

                val pdfBytes = genererVedtakPdf(vedtak)
                call.respondMedPDF(bytes = pdfBytes, filnavn = "vedtak-$vedtakId.pdf")
            } catch (e: Exception) {
                logger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding)
                sikkerLogger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_VEDTAK))
            }
        }
    }
}
