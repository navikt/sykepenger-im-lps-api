@file:UseSerializers(UuidSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
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
import no.nav.helsearbeidsgiver.auth.harTilgangTilMinstEnAvRessursene
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.metrikk.MetrikkDokumentType
import no.nav.helsearbeidsgiver.metrikk.tellApiRequest
import no.nav.helsearbeidsgiver.metrikk.tellDokumenterHentet
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.erDuplikat
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.opprettImTransaction
import no.nav.helsearbeidsgiver.utils.tilInnsending
import no.nav.helsearbeidsgiver.utils.tilInntektsmelding
import no.nav.helsearbeidsgiver.utils.tilSkjemaInntektsmelding
import no.nav.helsearbeidsgiver.utils.validerMotForespoersel
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr.Companion.erGyldig
import java.util.UUID

private const val VERSJON_1 = 1 // TODO: Skal denne settes / brukes?

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")
private val IM_RESSURS_GAMMEL = Env.getPropertyOrNull("ALTINN_IM_RESSURS_GAMMEL")

fun Route.inntektsmeldingV1(services: Services) {
    route("/v1") {
        sendInntektsmelding(services)
        filtrerInntektsmeldinger(services.inntektsmeldingService)
        hentInntektsmelding(services.inntektsmeldingService)
    }
}

private fun Route.sendInntektsmelding(services: Services) {
    // Send inn inntektsmelding
    post("/inntektsmelding") {
        try {
            val request = call.receive<InntektsmeldingRequest>()

            val forespoersel =
                services.forespoerselService.hentForespoersel(request.navReferanseId)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig NavReferanseId")

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilMinstEnAvRessursene(
                    ressurser = setOfNotNull(IM_RESSURS, IM_RESSURS_GAMMEL),
                    orgnumre = setOf(forespoersel.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }
            tellApiRequest()
            sikkerLogger().info("Mottatt innsending: $request")
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] sender inn skjema på vegne av bedrift: [${forespoersel.orgnr}] med systembrukerOrgnr: [$systembrukerOrgnr]",
            )

            request.valider().takeIf { it.isNotEmpty() }?.let {
                return@post call.respond(HttpStatusCode.BadRequest, it)
            }

            request.validerMotForespoersel(forespoersel)?.let {
                MdcUtils.withLogFields(
                    "hag_avsender_system_navn" to request.avsender.systemNavn,
                    "hag_avsender_system_versjon" to request.avsender.systemVersjon,
                    "hag_feilmelding" to it,
                ) {
                    sikkerLogger().warn("Mottatt ugyldig innsending: $it. Request: $request")
                    logger().warn("Mottatt ugyldig innsending: $it")
                }
                return@post call.respond(HttpStatusCode.BadRequest, it)
            }
            val sisteInntektsmelding =
                services.inntektsmeldingService
                    .hentNyesteInntektsmeldingByNavReferanseId(request.navReferanseId)
            val vedtaksperiodeId = services.forespoerselService.hentVedtaksperiodeId(request.navReferanseId)

            val inntektsmelding =
                request.tilInntektsmelding(
                    sluttbrukerOrgnr = Orgnr(forespoersel.orgnr),
                    lpsOrgnr = Orgnr(lpsOrgnr),
                    forespoersel = forespoersel,
                    vedtaksperiodeId = vedtaksperiodeId,
                )
            val eksponertForespoerselId =
                services.forespoerselService.hentEksponertForespoerselId(request.navReferanseId)
                    ?: request.navReferanseId

            val innsending =
                request.tilInnsending(inntektsmelding.id, eksponertForespoerselId, inntektsmelding.type, VERSJON_1)

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
    // Filtrer inntektsmeldinger på orgnr (underenhet), fnr, innsendingId, navReferanseId, status og/eller dato inntektsmeldingen ble mottatt av NAV.
    post("/inntektsmeldinger") {
        try {
            val filter = call.receive<InntektsmeldingFilter>()

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilMinstEnAvRessursene(
                    ressurser = setOfNotNull(IM_RESSURS, IM_RESSURS_GAMMEL),
                    orgnumre = setOf(filter.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }

            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            tellApiRequest()
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter inntektsmeldinger for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val inntektsmeldinger = inntektsmeldingService.hentInntektsMelding(filter = filter)
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.INNTEKTSMELDING, inntektsmeldinger.size)
            call.respondWithMaxLimit(inntektsmeldinger)
            return@post
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig filterparameter")
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, "Request mangler eller har ugyldig body")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}

private fun Route.hentInntektsmelding(inntektsmeldingService: InntektsmeldingService) {
    // Hent inntektsmelding med id
    get("/inntektsmelding/{innsendingId}") {
        try {
            val innsendingId = call.parameters["innsendingId"]?.let { UUID.fromString(it) }
            requireNotNull(innsendingId)

            val inntektsmelding =
                inntektsmeldingService
                    .hentInntektsmeldingMedInnsendingId(
                        innsendingId = innsendingId,
                    )

            if (inntektsmelding == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Inntektsmelding med innsendingId: $innsendingId ikke funnet.",
                )
                return@get
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilMinstEnAvRessursene(
                    ressurser = setOfNotNull(IM_RESSURS, IM_RESSURS_GAMMEL),
                    orgnumre = setOf(inntektsmelding.arbeidsgiver.orgnr, systembrukerOrgnr),
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }

            tellApiRequest()
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.INNTEKTSMELDING)

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter inntektsmelding med id $innsendingId for bedrift med " +
                    "systembrukerOrgnr: [$systembrukerOrgnr] og inntektsmeldingOrgnr: [${inntektsmelding.arbeidsgiver.orgnr}]",
            )

            call.respond(HttpStatusCode.OK, inntektsmelding)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av inntektsmeldinger: {$e}")
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av inntektsmeldinger")
        }
    }
}
