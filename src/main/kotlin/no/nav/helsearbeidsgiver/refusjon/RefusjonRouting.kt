package no.nav.helsearbeidsgiver.refusjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.CancellationException
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
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.respondMedPDF
import no.nav.helsearbeidsgiver.utils.toUuidOrNull

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

fun Route.refusjonV1(
    refusjonUtfallService: RefusjonUtfallService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/v1") {
        // TODO: Tilbyr ikke som JSON foreløpig
        // TODO: Vi må sjekke om LPS klient har tilgang til ressursen for dette endepunktet
        get("/refusjon/{refusjonUtfallId}/pdf") {
            if (!unleashFeatureToggles.skalEksponereRefusjonUtfallPdf()) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }
            val refusjonUtfall = hentRefusjonUtfallMedIdEllerError(refusjonUtfallService)
            if (refusjonUtfall != null) {
                val refusjonUtfallId = refusjonUtfall.refusjonUtfallId
                try {
                    val pdfBytes = refusjonUtfallService.hentRefusjonUtfallPdf(refusjonUtfallId)
                    if (pdfBytes == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse(FeilMedReferanse.REFUSJONUTFALL_IKKE_FUNNET, refusjonUtfallId))
                    } else {
                        call.respondMedPDF(bytes = pdfBytes, filnavn = "refusjon-$refusjonUtfallId.pdf")
                    }
                } catch (e: CancellationException) {
                    throw e // har ikke noe å si, utfallet blir likt, så bare kast igjen
                } catch (e: Exception) {
                    logger().error(Feil.FEIL_VED_HENTING_PDF.feilmelding)
                    sikkerLogger().error(Feil.FEIL_VED_HENTING_PDF.feilmelding, e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.EN_FEIL_OPPSTOD))
                }
            }
        }
    }
}

private suspend fun RoutingContext.hentRefusjonUtfallMedIdEllerError(
    refusjonUtfallService: RefusjonUtfallService,
): RefusjonUtfallMetadata? {
    try {
        val tokenContext = tokenValidationContext()
        val lpsOrgnr = tokenContext.getConsumerOrgnr()
        val systembrukerOrgnr = tokenContext.getSystembrukerOrgnr()
        val refusjonUtfallId = call.parameters["refusjonUtfallId"]?.toUuidOrNull()
        if (refusjonUtfallId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_REFUSJONUTFALL_ID))
            return null
        }

        val refusjonUtfall = refusjonUtfallService.hentRefusjonUtfallMetadata(refusjonUtfallId)
        if (refusjonUtfall == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse(FeilMedReferanse.REFUSJONUTFALL_IKKE_FUNNET, refusjonUtfallId))
            return null
        }

        if (!tokenContext.harTilgangTilRessurs(
                ressurs = IM_RESSURS,
                orgnr = refusjonUtfall.orgnr.verdi,
            )
        ) {
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
            return null
        }

        sikkerLogger().debug(
            "LPS: [{}] henter refusjonsutfall med id: [{}] på vegne av orgnr: {}",
            lpsOrgnr,
            refusjonUtfallId,
            systembrukerOrgnr,
        )
        return refusjonUtfall
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger().error(Feil.HENTING_REFUSJONUTFALL.feilmelding)
        sikkerLogger().error(Feil.HENTING_REFUSJONUTFALL.feilmelding, e)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.HENTING_REFUSJONUTFALL))
        return null
    }
}

fun Route.refusjonTokenX(
    refusjonUtfallService: RefusjonUtfallService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/intern/personbruker") {
        get("/refusjon/{refusjonUtfallId}/pdf") {
            try {
                if (!unleashFeatureToggles.skalEksponereRefusjonUtfallPdf()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }

                val tokenContext = tokenValidationContext()
                val pid = tokenContext.getPidFromTokenX()

                if (pid == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.MANGLER_BRUKERIDENTIFIKASJON))
                    return@get
                }

                val refusjonUtfallId = call.parameters["refusjonUtfallId"]?.toUuidOrNull()
                if (refusjonUtfallId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_REFUSJONUTFALL_ID))
                    return@get
                }

                val refusjon = refusjonUtfallService.hentRefusjonUtfallMetadata(refusjonUtfallId)
                if (refusjon == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse(FeilMedReferanse.REFUSJONUTFALL_IKKE_FUNNET, refusjonUtfallId),
                    )
                    return@get
                }

                if (!tokenContext.personHarTilgangTilRessurs(
                        ressurs = IM_RESSURS,
                        orgnr = refusjon.orgnr.verdi,
                        pid = pid,
                    )
                ) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
                    return@get
                }

                sikkerLogger().info("Bruker med PID: $pid henter refusjonUtfall PDF: $refusjonUtfallId")

                val pdfBytes = refusjonUtfallService.hentRefusjonUtfallPdf(refusjon.refusjonUtfallId)
                if (pdfBytes == null) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_PDF))
                    return@get
                }
                call.respondMedPDF(bytes = pdfBytes, filnavn = "refusjon-$refusjonUtfallId.pdf")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger().error(Feil.HENTING_REFUSJONUTFALL.feilmelding)
                sikkerLogger().error(Feil.HENTING_REFUSJONUTFALL.feilmelding, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.HENTING_REFUSJONUTFALL))
            }
        }
    }
}
