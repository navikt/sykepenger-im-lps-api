package no.nav.helsearbeidsgiver.forespoersel

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
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_FORESPOERSEL
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_FORESPOERSLER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.IKKE_TILGANG_TIL_RESSURS
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_FILTERPARAMETER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_IDENTIFIKATOR
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_NAV_REFERANSE_ID
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_REQUEST_BODY
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

fun Route.forespoerselV1(
    forespoerselService: ForespoerselService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/v1") {
        forespoersel(forespoerselService, unleashFeatureToggles)
        filtrerForespoersler(forespoerselService, unleashFeatureToggles)
    }
}

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

private fun Route.forespoersel(
    forespoerselService: ForespoerselService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Hent forespørsel med navReferanseId.
    get("/forespoersel/{navReferanseId}") {
        if (!unleashFeatureToggles.skalEksponereForespoersler()) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        try {
            val navReferanseId = call.parameters["navReferanseId"]?.toUuidOrNull()
            if (navReferanseId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_NAV_REFERANSE_ID))
                return@get
            }

            val forespoersel = forespoerselService.hentForespoersel(navReferanseId)
            if (forespoersel == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Forespørsel med navReferanseId: $navReferanseId ikke funnet."))
                return@get
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnr = forespoersel.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
                return@get
            }

            tellApiRequest()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsel med id $navReferanseId for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                    " og forespørselOrgnr: [${forespoersel.orgnr}]",
            )
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.FORESPOERSEL)

            call.respond(forespoersel)
        } catch (e: Exception) {
            FEIL_VED_HENTING_FORESPOERSEL.also {
                logger().error(it)
                sikkerLogger().error(it, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(it))
            }
        }
    }
}

private fun Route.filtrerForespoersler(
    forespoerselService: ForespoerselService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Filtrer forespørsler om inntektsmelding på orgnr (underenhet), fnr, navReferanseId, status og/eller dato forespørselen ble opprettet av NAV.
    post("/forespoersler") {
        if (!unleashFeatureToggles.skalEksponereForespoersler()) {
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }
        try {
            val filter = call.receive<ForespoerselFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnr = filter.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
                return@post
            }

            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            tellApiRequest()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsler for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val forespoersler = forespoerselService.filtrerForespoersler(filter)
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.FORESPOERSEL, forespoersler.size)
            call.respondWithMaxLimit(forespoersler)
            return@post
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_IDENTIFIKATOR))
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_REQUEST_BODY))
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_FILTERPARAMETER))
        } catch (e: Exception) {
            sikkerLogger().error(FEIL_VED_HENTING_FORESPOERSLER, e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(FEIL_VED_HENTING_FORESPOERSLER))
        }
    }
}
