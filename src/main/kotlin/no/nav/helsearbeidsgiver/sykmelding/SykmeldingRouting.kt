package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
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
import no.nav.helsearbeidsgiver.auth.harTilgangTilMinstEnAvRessursene
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.metrikk.MetrikkDokumentType
import no.nav.helsearbeidsgiver.metrikk.tellApiRequest
import no.nav.helsearbeidsgiver.metrikk.tellDokumenterHentet
import no.nav.helsearbeidsgiver.plugins.respondWithMaxLimit
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

private val SM_RESSURS = Env.getProperty("ALTINN_SM_RESSURS")
private val SM_RESSURS_GAMMEL = Env.getPropertyOrNull("ALTINN_SM_RESSURS_GAMMEL")

fun Route.sykmeldingV1(
    sykmeldingService: SykmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    route("/v1") {
        sykmelding(sykmeldingService, unleashFeatureToggles)
        filtrerSykmeldinger(sykmeldingService, unleashFeatureToggles)
    }
}

private fun Route.sykmelding(
    sykmeldingService: SykmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Hent sykmelding med sykmeldingId
    get("/sykmelding/{sykmeldingId}") {
        try {
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()

            if (!unleashFeatureToggles.skalEksponereSykmeldinger(orgnr = Orgnr(lpsOrgnr))) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            val sykmeldingId = call.parameters["sykmeldingId"]?.toUuidOrNull()
            requireNotNull(sykmeldingId) { "navReferanseId: $sykmeldingId ikke gyldig UUID" }

            val sykmelding = sykmeldingService.hentSykmelding(sykmeldingId)
            if (sykmelding == null) {
                call.respond(NotFound, "Sykmelding med id: $sykmeldingId ikke funnet.")
                return@get
            }

            if (!tokenValidationContext().harTilgangTilMinstEnAvRessursene(
                    ressurser = setOfNotNull(SM_RESSURS, SM_RESSURS_GAMMEL),
                    orgnr = sykmelding.arbeidsgiver.orgnr.verdi,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@get
            }
            tellApiRequest()
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykmelding [$sykmeldingId] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]" +
                    " og sykmeldingOrgnr: [${sykmelding.arbeidsgiver.orgnr}]",
            )
            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKMELDING)
            call.respond(sykmelding)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykmelding", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykmelding")
        }
    }
}

private fun Route.filtrerSykmeldinger(
    sykmeldingService: SykmeldingService,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    // Filtrer sykmeldinger på orgnr (underenhet), fnr og/eller dato sykmeldingen ble mottatt av NAV.
    post("/sykmeldinger") {
        try {
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr()

            if (!unleashFeatureToggles.skalEksponereSykmeldinger(orgnr = Orgnr(lpsOrgnr))) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }

            val filter = call.receive<SykmeldingFilter>()
            val systembrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr().also { require(Orgnr.erGyldig(it)) }

            if (!tokenValidationContext().harTilgangTilMinstEnAvRessursene(
                    ressurser = setOfNotNull(SM_RESSURS, SM_RESSURS_GAMMEL),
                    orgnr = filter.orgnr,
                )
            ) {
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til ressurs")
                return@post
            }

            tellApiRequest()
            sikkerLogger().info(
                "LPS: [$lpsOrgnr] henter sykmeldinger for orgnr [${filter.orgnr}] for bedrift med systembrukerOrgnr: [$systembrukerOrgnr]",
            )
            val sykemeldinger = sykmeldingService.hentSykmeldinger(filter)

            tellDokumenterHentet(lpsOrgnr, MetrikkDokumentType.SYKMELDING, antall = sykemeldinger.size)
            call.respondWithMaxLimit(sykemeldinger)
            return@post
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig identifikator")
        } catch (_: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig filterparameter")
        } catch (_: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, "Request mangler eller har ugyldig body")
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av sykmeldinger", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av sykmeldinger")
        }
    }
}
