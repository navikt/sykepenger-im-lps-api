package no.nav.helsearbeidsgiver.forespoersel

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
