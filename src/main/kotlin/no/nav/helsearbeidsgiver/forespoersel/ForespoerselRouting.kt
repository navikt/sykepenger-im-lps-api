package no.nav.helsearbeidsgiver.forespoersel

import io.ktor.http.HttpStatusCode
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
import no.nav.helsearbeidsgiver.metrikk.tellDokumentHentetMedMaxAntall
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

fun Route.forespoerselV1(forespoerselService: ForespoerselService) {
    route("/v1") {
        forespoersel(forespoerselService)
        filtrerForespoersler(forespoerselService)
    }
}

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

private fun Route.forespoersel(forespoerselService: ForespoerselService) {
    // Hent forespørsel med navReferanseId.
    get("/forespoersel/{navReferanseId}") {
        try {
            val navReferanseId = call.parameters["navReferanseId"]?.toUuidOrNull()
            requireNotNull(navReferanseId) { "navReferanseId: $navReferanseId ikke gyldig UUID" }

            val forespoersel = forespoerselService.hentForespoersel(navReferanseId)
            if (forespoersel == null) {
                call.respond(HttpStatusCode.NotFound, "Forespørsel med navReferanseId: $navReferanseId ikke funnet.")
                return@get
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnumre = setOf(forespoersel.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }

            tellApiRequest()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsel med id $navReferanseId for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                    " og forespørselOrgnr: [${forespoersel.orgnr}]",
            )
            tellDokumentHentetMedMaxAntall(lpsOrgnr, MetrikkDokumentType.FORESPOERSEL)
            call.respond(forespoersel)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}

private fun Route.filtrerForespoersler(forespoerselService: ForespoerselService) {
    // Filtrer forespørsler om inntektsmelding på orgnr (underenhet), fnr, navReferanseId, status og/eller dato forespørselen ble opprettet av NAV.
    post("/forespoersler") {
        try {
            val filter = call.receive<ForespoerselFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnumre = setOf(filter.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }

            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            tellApiRequest()

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter forespørsler for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val forespoersler = forespoerselService.filtrerForespoersler(filter)
            tellDokumentHentetMedMaxAntall(lpsOrgnr, MetrikkDokumentType.FORESPOERSEL, forespoersler.size)
            call.respondWithMaxLimit(forespoersler)
            return@post
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler")
        }
    }
}
