@file:UseSerializers(UuidSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.utils.erDuplikat
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.opprettImTransaction
import no.nav.helsearbeidsgiver.utils.tilInnsending
import no.nav.helsearbeidsgiver.utils.tilInntektsmelding
import no.nav.helsearbeidsgiver.utils.tilSkjemaInntektsmelding
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

private const val VERSJON_1 = 1 // TODO: Skal denne settes / brukes?

fun Route.inntektsmeldingV1(services: Services) {
    route("/v1") {
        filtrerInntektsmeldinger(services.inntektsmeldingService)
        inntektsmeldinger(services.inntektsmeldingService)
        innsending(services)
    }
}

private fun Route.innsending(services: Services) {
    // Send inn inntektsmelding
    post("/inntektsmelding") {
        try {
            val request = call.receive<InntektsmeldingRequest>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            sikkerLogger().info("Mottatt innsending: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] sender inn skjema på vegne av bedrift: [$sluttbrukerOrgnr]")

            request.valider().takeIf { it.isNotEmpty() }?.let {
                return@post call.respond(HttpStatusCode.BadRequest, it)
            }

            val forespoersel = services.forespoerselService.hentForespoersel(request.navReferanseId)
            if (forespoersel == null || forespoersel.orgnr != sluttbrukerOrgnr) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }

            val sisteInntektsmelding =
                services.inntektsmeldingService
                    .hentNyesteInntektsmeldingByNavRefernaseId(request.navReferanseId)

            val inntektsmelding =
                request.tilInntektsmelding(
                    sluttbrukerOrgnr = Orgnr(sluttbrukerOrgnr),
                    lpsOrgnr = Orgnr(lpsOrgnr),
                    forespoersel = forespoersel,
                )
            val innsending = request.tilInnsending(inntektsmelding.type, VERSJON_1)

            when {
                sisteInntektsmelding == null && innsending.aarsakInnsending == AarsakInnsending.Endring ->
                    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig aarsak innsending")
                sisteInntektsmelding != null && innsending.aarsakInnsending == AarsakInnsending.Ny ->
                    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig aarsak innsending")
                sisteInntektsmelding != null &&
                    innsending.skjema.erDuplikat(
                        sisteInntektsmelding.tilSkjemaInntektsmelding(),
                    ) -> return@post call.respond(HttpStatusCode.Conflict, "Duplikat forrige innsending")
            }

            services.opprettImTransaction(inntektsmelding, innsending)
            call.respond(HttpStatusCode.Created, inntektsmelding.id.toString())
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved lagring av innsending: {$e}", e)
            call.respond(HttpStatusCode.InternalServerError, "En feil oppstod")
        }
    }
}

private fun Route.filtrerInntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    // Hent inntektsmeldinger for tilhørende systembrukers orgnr, filtrer basert på request
    post("/inntektsmeldinger") {
        try {
            val request = call.receive<InntektsmeldingFilterRequest>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("Mottatt request: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$sluttbrukerOrgnr]")
            inntektsmeldingService
                .hentInntektsMeldingByRequest(
                    orgnr = sluttbrukerOrgnr,
                    request = request,
                ).takeIf { it.antall > 0 }
                ?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.NotFound, "Ingen inntektsmeldinger funnet")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}

private fun Route.inntektsmeldinger(inntektsmeldingService: InntektsmeldingService) {
    // Hent alle inntektsmeldinger for tilhørende systembrukers orgnr
    get("/inntektsmeldinger") {
        try {
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$sluttbrukerOrgnr]")
            inntektsmeldingService
                .hentInntektsmeldingerByOrgNr(sluttbrukerOrgnr)
                .takeIf { it.antall > 0 }
                ?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.NotFound, "Ingen inntektsmeldinger funnet")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}
