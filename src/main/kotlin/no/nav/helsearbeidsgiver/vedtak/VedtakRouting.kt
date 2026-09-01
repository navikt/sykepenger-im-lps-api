package no.nav.helsearbeidsgiver.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.getPidFromTokenX
import no.nav.helsearbeidsgiver.auth.personHarTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.Feil
import no.nav.helsearbeidsgiver.plugins.FeilMedReferanse
import no.nav.helsearbeidsgiver.utils.genererVedtakPdf
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.respondMedPDF
import no.nav.helsearbeidsgiver.utils.toUuidOrNull

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

fun Route.vedtakTokenX(vedtakService: VedtakService) {
    route("/intern/personbruker") {
        get("/vedtak/{vedtakId}/pdf") {
            try {
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
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
                    return@get
                }

                sikkerLogger().info("Bruker med PID: $pid henter vedtak PDF: $vedtakId")

                val pdfBytes = genererVedtakPdf(vedtak.vedtakForPdf)
                call.respondMedPDF(bytes = pdfBytes, filnavn = "vedtak-$vedtakId.pdf")
            } catch (e: Exception) {
                logger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding)
                sikkerLogger().error(Feil.FEIL_VED_HENTING_VEDTAK.feilmelding, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.FEIL_VED_HENTING_VEDTAK))
            }
        }
    }
}
