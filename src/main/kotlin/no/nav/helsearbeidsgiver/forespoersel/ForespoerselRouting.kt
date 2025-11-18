package no.nav.helsearbeidsgiver.forespoersel

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
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.SykepengerApiException
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.receiveFilter
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
        val navReferanseId =
            call.parameters["navReferanseId"]?.toUuidOrNull()
                ?: throw SykepengerApiException.InvalidUuid("Ugyldig navReferanseId")

        val forespoersel =
            forespoerselService.hentForespoersel(navReferanseId)
                ?: throw SykepengerApiException.NotFound("Forespørsel med navReferanseId: $navReferanseId ikke funnet.")

        val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

        if (!tokenValidationContext().harTilgangTilRessurs(
                ressurs = IM_RESSURS,
                orgnr = forespoersel.orgnr,
            )
        ) {
            throw SykepengerApiException.Unauthorized("Ikke tilgang til ressurs")
        }

        sikkerLogger().info(
            "LPS: [$lpsOrgnr] henter forespørsel med id $navReferanseId for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                " og forespørselOrgnr: [${forespoersel.orgnr}]",
        )

        tellApiRequest()
        tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.FORESPOERSEL)

        call.respond(forespoersel)
    }
}

private fun Route.filtrerForespoersler(forespoerselService: ForespoerselService) {
    // Filtrer forespørsler om inntektsmelding på orgnr (underenhet), fnr, navReferanseId, status og/eller dato forespørselen ble opprettet av NAV.
    post("/forespoersler") {
        val filter = call.receiveFilter<ForespoerselFilter>()

        val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
        if (!Orgnr.erGyldig(systembrukerOrgnr)) {
            throw SykepengerApiException.BadRequest("Ugyldig identifikator")
        }

        if (!tokenValidationContext().harTilgangTilRessurs(
                ressurs = IM_RESSURS,
                orgnr = filter.orgnr,
            )
        ) {
            throw SykepengerApiException.Unauthorized("Ikke tilgang til ressurs")
        }

        val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

        sikkerLogger().info(
            "LPS: [$lpsOrgnr] henter forespørsler for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
        )

        val forespoersler = forespoerselService.filtrerForespoersler(filter)

        tellApiRequest()
        tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.FORESPOERSEL, forespoersler.size)

        call.respondWithMaxLimit(forespoersler)
    }
}
