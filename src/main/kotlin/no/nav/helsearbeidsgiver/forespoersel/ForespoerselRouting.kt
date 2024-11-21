package no.nav.helsearbeidsgiver.forespoersel

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSupplierOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun Route.forespoersler(forespoerselService: ForespoerselService) {
    get("/forespoersler") {
        val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
        val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
        if (consumerOrgnr != null) {
            sikkerLogger().info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$consumerOrgnr]")
            call.respond(forespoerselService.hentForespoerslerForOrgnr(consumerOrgnr))
        } else {
            sikkerLogger().warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
            call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
        }
    }
}

fun Route.filtererForespoersler(forespoerselService: ForespoerselService) {
    post("/forespoersler") {
        try {
            val request = call.receive<ForespoerselRequest>()
            val consumerOrgnr = tokenValidationContext().getConsumerOrgnr()
            val lpsOrgnr = tokenValidationContext().getSupplierOrgnr()
            if (consumerOrgnr != null) {
                sikkerLogger().info("LPS: [$lpsOrgnr] henter forespørsler for bedrift: [$consumerOrgnr]")
                call.respond(forespoerselService.filtrerForespoerslerForOrgnr(consumerOrgnr, request))
            } else {
                sikkerLogger().warn("LPS: [$lpsOrgnr] - Consumer orgnr mangler")
                call.respond(HttpStatusCode.Unauthorized, "Consumer orgnr mangler")
            }
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved henting av forespørsler", e)
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av forespørsler - ${e.message}")
        }
    }
}
