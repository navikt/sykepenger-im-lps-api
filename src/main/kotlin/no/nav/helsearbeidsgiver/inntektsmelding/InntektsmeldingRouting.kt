@file:UseSerializers(UuidSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.erDuplikat
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.opprettImTransaction
import no.nav.helsearbeidsgiver.utils.tilInnsending
import no.nav.helsearbeidsgiver.utils.tilInntektsmelding
import no.nav.helsearbeidsgiver.utils.tilSkjemaInntektsmelding
import no.nav.helsearbeidsgiver.utils.validerMotForespoersel
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

private const val VERSJON_1 = 1 // TODO: Skal denne settes / brukes?

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

fun Route.inntektsmeldingV1(services: Services) {
    route("/v1") {
        filtrerInntektsmeldinger(services.inntektsmeldingService)
        inntektsmeldinger(services.inntektsmeldingService)
        innsending(services)
        inntektsmelding(services.inntektsmeldingService)
    }
}

private fun Route.innsending(services: Services) {
    // Send inn inntektsmelding
    post("/inntektsmelding") {
        try {
            val request = call.receive<InntektsmeldingRequest>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }
            sikkerLogger().info("Mottatt innsending: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] sender inn skjema på vegne av bedrift: [$sluttbrukerOrgnr]")

            request.valider().takeIf { it.isNotEmpty() }?.let {
                return@post call.respond(HttpStatusCode.BadRequest, it)
            }

            val forespoersel =
                services.forespoerselService.hentForespoersel(request.navReferanseId, sluttbrukerOrgnr)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig NavReferanseId")

            request.validerMotForespoersel(forespoersel, sluttbrukerOrgnr)?.let {
                sikkerLogger().warn("Mottatt ugyldig innsending: $it. Request: $request")
                return@post call.respond(HttpStatusCode.BadRequest, it)
            }
            val sisteInntektsmelding =
                services.inntektsmeldingService
                    .hentNyesteInntektsmeldingByNavReferanseId(request.navReferanseId)
            val vedtaksperiodeId = services.forespoerselService.hentVedtaksperiodeId(request.navReferanseId)

            val inntektsmelding =
                request.tilInntektsmelding(
                    sluttbrukerOrgnr = Orgnr(sluttbrukerOrgnr),
                    lpsOrgnr = Orgnr(lpsOrgnr),
                    forespoersel = forespoersel,
                    vedtaksperiodeId = vedtaksperiodeId,
                )
            val eksponertForespoerselId = services.forespoerselService.hentEksponertForespoerselId(request.navReferanseId)
            val innsending = request.tilInnsending(inntektsmelding.id, eksponertForespoerselId, inntektsmelding.type, VERSJON_1)

            if (
                sisteInntektsmelding != null &&
                innsending.skjema.erDuplikat(
                    sisteInntektsmelding.tilSkjemaInntektsmelding(eksponertForespoerselId),
                )
            ) {
                return@post call.respond(HttpStatusCode.Conflict, sisteInntektsmelding.id.toString())
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
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$sluttbrukerOrgnr]")
            inntektsmeldingService
                .hentInntektsMeldingByRequest(
                    orgnr = sluttbrukerOrgnr,
                    request = request,
                ).let {
                    call.respond(it)
                }
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
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmeldinger for bedrift: [$sluttbrukerOrgnr]")
            inntektsmeldingService
                .hentInntektsmeldingerByOrgNr(sluttbrukerOrgnr)
                .let {
                    call.respond(it)
                }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}

private fun Route.inntektsmelding(inntektsmeldingService: InntektsmeldingService) {
    // Hent inntektsmelding med id
    get("/inntektsmelding/{inntektsmeldingId}") {
        try {
            val inntektsmeldingId = call.parameters["inntektsmeldingId"]?.let { UUID.fromString(it) }
            requireNotNull(inntektsmeldingId)
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmelding med id: [$inntektsmeldingId]")
            val inntektsmelding =
                inntektsmeldingService
                    .hentInntektsmeldingMedInnsendingId(
                        sluttbrukerOrgnr,
                        innsendingId = inntektsmeldingId,
                    )
            if (inntektsmelding != null) {
                call.respond(HttpStatusCode.OK, inntektsmelding)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
    // Hent alle inntektsmeldinger med navReferanseId
    get("/inntektsmelding/navReferanseId/{navReferanseId}") {
        try {
            val navReferanseId = call.parameters["navReferanseId"]?.let { UUID.fromString(it) }
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmelding med navReferanseId: [$navReferanseId]")
            inntektsmeldingService
                .hentInntektsMeldingByRequest(
                    sluttbrukerOrgnr,
                    InntektsmeldingFilterRequest(
                        navReferanseId = navReferanseId,
                    ),
                ).let {
                    call.respond(it)
                }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
    // Hent alle inntektsmeldinger med status
    get("/inntektsmelding/status/{status}") {
        try {
            val status = call.parameters["status"]?.let { InnsendingStatus.valueOf(it) }
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            if (!tokenValidationContext().harTilgangTilRessurs(IM_RESSURS)) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            sikkerLogger().info("LPS: [$lpsOrgnr] henter inntektsmelding med status: [$status]")
            inntektsmeldingService
                .hentInntektsMeldingByRequest(
                    sluttbrukerOrgnr,
                    InntektsmeldingFilterRequest(
                        status = status,
                    ),
                ).let {
                    call.respond(it)
                }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}
