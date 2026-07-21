package no.nav.helsearbeidsgiver.inntekt

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.metrikk.tellApiRequest
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.Feil
import no.nav.helsearbeidsgiver.plugins.FeilMedReferanse
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")
private val INNTEKTSDATO_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE

fun Route.inntektV1(services: Services) {
    route("/v1") {
        hentInntekt(services)
    }
}

private fun Route.hentInntekt(services: Services) {
    get("/inntekt") {
        val navReferanseId = call.request.queryParameters["navReferanseId"]?.toUuidOrNull()
        if (navReferanseId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_NAV_REFERANSE_ID))
            return@get
        }

        val inntektsdato =
            runCatching {
                call.request.queryParameters["inntektsdato"]
                    ?.let { LocalDate.parse(it, INNTEKTSDATO_FORMATTER) }
            }.getOrNull()

        if (inntektsdato == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(Feil.UGYLDIG_DATO))
            return@get
        }

        try {
            val forespoersel = services.forespoerselService.hentForespoersel(navReferanseId)
            if (forespoersel == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse(FeilMedReferanse.FORESPOERSEL_IKKE_FUNNET, navReferanseId))
                return@get
            }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnr = forespoersel.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(Feil.IKKE_TILGANG_TIL_RESSURS))
                return@get
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            tellApiRequest()

            val inntekter =
                services.inntektService
                    .hentInntekter(
                        navReferanseId = navReferanseId,
                        inntektsdato = inntektsdato,
                    )

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter inntekt for navReferanseId [$navReferanseId] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )

            call.respond(inntekter)
        } catch (e: Exception) {
            logger().error("Feil ved henting av inntekt")
            sikkerLogger().error("Feil ved henting av inntekt", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(Feil.EN_FEIL_OPPSTOD))
        }
    }
}
