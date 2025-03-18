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
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Avsender
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykmeldt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.OffsetDateTime
import java.util.UUID

fun Route.inntektsmeldingV1(
    inntektsmeldingService: InntektsmeldingService,
    innsendingService: InnsendingService,
) {
    route("/v1") {
        filtrerInntektsmeldinger(inntektsmeldingService)
        inntektsmeldinger(inntektsmeldingService)
        innsending(inntektsmeldingService, innsendingService)
    }
}

private fun Route.innsending(
    inntektsmeldingService: InntektsmeldingService,
    innsendingService: InnsendingService,
) {
    // Send inn inntektsmelding
    post("/inntektsmelding") {
        try {
            val request = this.call.receive<InntektsmeldingRequest>()
            val sluttbrukerOrgnr = tokenValidationContext().getSystembrukerOrgnr()
            val lpsOrgnr = tokenValidationContext().getConsumerOrgnr() // TODO Underenhet!
            // Hvis tilgang gis til hovedenhet, må man gjøre noe med tilganger til underenheter etc...

            sikkerLogger().info("Mottatt innsending: $request")
            sikkerLogger().info("LPS: [$lpsOrgnr] sender inn skjema på vegne av bedrift: [$sluttbrukerOrgnr]")

            val versjon = 1

            request.valider().takeIf { it.isNotEmpty() }?.let {
                call.respond(HttpStatusCode.BadRequest, it)
                return@post
            }
            val inntektsmelding =
                Inntektsmelding(
                    id = UUID.randomUUID(),
                    type = Inntektsmelding.Type.Forespurt(request.navReferanseId),
                    sykmeldt =
                        Sykmeldt(
                            Fnr(request.sykmeldtFnr),
                            "",
                        ),
                    // TODO
                    avsender =
                        Avsender(
                            Orgnr(sluttbrukerOrgnr),
                            "",
                            "",
                            request.arbeidsgiverTlf,
                        ),
                    sykmeldingsperioder = emptyList(), // TODO hent fra forespørsel
                    agp = request.agp,
                    inntekt = request.inntekt,
                    refusjon = request.refusjon,
                    aarsakInnsending = AarsakInnsending.Ny,
                    mottatt = OffsetDateTime.now(),
                    vedtaksperiodeId = null, // TODO: slå opp fra forespørsel
                )
            // TODO: transaction funker ikke nu, vi satser på at det går bra :)
            inntektsmeldingService.opprettInntektsmelding(
                im = inntektsmelding,
                systemNavn = request.avsender.systemNavn,
                systemVersjon = request.avsender.systemVersjon,
                innsendingStatus = InnsendingStatus.MOTTATT,
            )
            innsendingService.lagreBakgrunsjobbInnsending(
                SkjemaInntektsmelding(
                    forespoerselId = request.navReferanseId,
                    avsenderTlf = request.arbeidsgiverTlf,
                    agp = request.agp,
                    inntekt = request.inntekt,
                    refusjon = request.refusjon,
                ),
            )
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
