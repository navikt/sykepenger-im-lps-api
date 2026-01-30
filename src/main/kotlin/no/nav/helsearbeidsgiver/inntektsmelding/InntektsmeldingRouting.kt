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
import no.nav.helsearbeidsgiver.auth.harTilgangTilRessurs
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.metrikk.MetrikkDokumentType
import no.nav.helsearbeidsgiver.metrikk.tellApiRequest
import no.nav.helsearbeidsgiver.metrikk.tellDokumenterHentet
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.EN_FEIL_OPPSTOD
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_INNTEKTSMELDING
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.FEIL_VED_HENTING_INNTEKTSMELDINGER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.IKKE_TILGANG_TIL_RESSURS
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_FILTERPARAMETER
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_INNSENDING_ID
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_NAV_REFERANSE_ID
import no.nav.helsearbeidsgiver.plugins.ErrorMessages.UGYLDIG_REQUEST_BODY
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.erDuplikat
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.opprettImTransaction
import no.nav.helsearbeidsgiver.utils.tilInnsending
import no.nav.helsearbeidsgiver.utils.tilInntektsmelding
import no.nav.helsearbeidsgiver.utils.tilSkjemaInntektsmelding
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.validerMotForespoersel
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr.Companion.erGyldig

private const val VERSJON_1 = 1 // TODO: Skal denne settes / brukes?

private val IM_RESSURS = Env.getProperty("ALTINN_IM_RESSURS")

fun Route.inntektsmeldingV1(
    services: Services,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/v1") {
        sendInntektsmelding(services, unleashFeatureToggles)
        filtrerInntektsmeldinger(services.inntektsmeldingService, unleashFeatureToggles)
        hentInntektsmelding(services.inntektsmeldingService, unleashFeatureToggles)
    }
}

private fun Route.sendInntektsmelding(
    services: Services,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Send inn inntektsmelding

    post("/inntektsmelding") {
        if (!unleashFeatureToggles.skalEksponereInntektsmeldinger()) {
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }
        try {
            val request = call.receive<InntektsmeldingRequest>()
            val forespoersel =
                services.forespoerselService.hentForespoersel(request.navReferanseId)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_NAV_REFERANSE_ID))

            val sisteForespoersel = services.forespoerselService.hentSisteForespoersel(forespoersel)
            if (sisteForespoersel.navReferanseId != forespoersel.navReferanseId) {
                val feilmelding =
                    "Det finnes en nyere forespørsel for vedtaksperioden. Nyeste forespørsel: ${sisteForespoersel.navReferanseId}"
                MdcUtils.withLogFields(
                    "hag_avsender_system_navn" to request.avsender.systemNavn,
                    "hag_avsender_system_versjon" to request.avsender.systemVersjon,
                    "hag_feilmelding" to feilmelding,
                ) {
                    sikkerLogger().warn("Mottatt ugyldig innsending. Request: $request")
                    logger().warn("Mottatt ugyldig innsending: $feilmelding")
                }
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(feilmelding))
            }
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnr = forespoersel.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
                return@post
            }
            tellApiRequest()
            sikkerLogger().info("Mottatt innsending: $request")
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] sender inn skjema på vegne av bedrift: [${forespoersel.orgnr}] med systembrukerOrgnr: [$systembrukerOrgnr]",
            )

            request.valider().takeIf { it.isNotEmpty() }?.let {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.joinToString("; ")))
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
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(it))
            }
            val sisteInntektsmelding =
                services.inntektsmeldingService
                    .hentNyesteInntektsmeldingByNavReferanseId(request.navReferanseId)

            val inntektsmelding =
                request.tilInntektsmelding(
                    sluttbrukerOrgnr = Orgnr(forespoersel.orgnr),
                    lpsOrgnr = Orgnr(lpsOrgnr),
                    forespoersel = forespoersel,
                    vedtaksperiodeId = forespoersel.vedtaksperiodeId,
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
                return@post call.respond(HttpStatusCode.Conflict, ErrorResponse(sisteInntektsmelding.id.toString()))
            }

            services.opprettImTransaction(inntektsmelding, innsending)
            call.respond(HttpStatusCode.Created, InnsendingResponse(inntektsmelding.id.toString()))
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved lagring av innsending: {$e}", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(EN_FEIL_OPPSTOD))
        }
    }
}

private fun Route.filtrerInntektsmeldinger(
    inntektsmeldingService: InntektsmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Filtrer inntektsmeldinger på orgnr (underenhet), fnr, innsendingId, navReferanseId, status og/eller dato inntektsmeldingen ble mottatt av NAV.
    post("/inntektsmeldinger") {
        if (!unleashFeatureToggles.skalEksponereInntektsmeldinger()) {
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }
        try {
            val filter = call.receive<InntektsmeldingFilter>()

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(erGyldig(it)) }

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
                "LPS: [$lpsOrgnr] henter inntektsmeldinger for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val inntektsmeldinger = inntektsmeldingService.hentInntektsMelding(filter = filter)
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.INNTEKTSMELDING, inntektsmeldinger.size)
            call.respondWithMaxLimit(inntektsmeldinger)
            return@post
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_FILTERPARAMETER))
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_REQUEST_BODY))
        } catch (e: Exception) {
            sikkerLogger().error("${FEIL_VED_HENTING_INNTEKTSMELDINGER}: {$e}")
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(FEIL_VED_HENTING_INNTEKTSMELDINGER))
        }
    }
}

private fun Route.hentInntektsmelding(
    inntektsmeldingService: InntektsmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Hent inntektsmelding med id
    get("/inntektsmelding/{innsendingId}") {
        if (!unleashFeatureToggles.skalEksponereInntektsmeldinger()) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        try {
            val innsendingId = call.parameters["innsendingId"]?.toUuidOrNull()
            if (innsendingId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(UGYLDIG_INNSENDING_ID))
                return@get
            }

            val inntektsmelding =
                inntektsmeldingService
                    .hentInntektsmeldingMedInnsendingId(
                        innsendingId = innsendingId,
                    )

            if (inntektsmelding == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Inntektsmelding med innsendingId: $innsendingId ikke funnet."),
                )
                return@get
            }

            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!tokenValidationContext().harTilgangTilRessurs(
                    ressurs = IM_RESSURS,
                    orgnr = inntektsmelding.arbeidsgiver.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(IKKE_TILGANG_TIL_RESSURS))
                return@get
            }

            tellApiRequest()
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.INNTEKTSMELDING)

            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter inntektsmelding med id $innsendingId for bedrift med " +
                    "systembrukerOrgnr: [$systembrukerOrgnr] og inntektsmeldingOrgnr: [${inntektsmelding.arbeidsgiver.orgnr}]",
            )

            call.respond(inntektsmelding)
        } catch (e: Exception) {
            FEIL_VED_HENTING_INNTEKTSMELDING.also {
                logger().error(it)
                sikkerLogger().error(it, e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(it))
            }
        }
    }
}
