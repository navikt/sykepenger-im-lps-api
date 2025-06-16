package no.nav.helsearbeidsgiver.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.forespoersel.forespoerselV1
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldingV1
import no.nav.helsearbeidsgiver.soeknad.soeknadV1
import no.nav.helsearbeidsgiver.sykmelding.sykmeldingV1
import org.jetbrains.exposed.sql.Database

fun Application.configureRouting(
    services: Services,
    db: Database,
) {
    routing {
        naisRoutes(db)
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate("systembruker-config") {
            inntektsmeldingV1(
                services = services,
            )
            forespoerselV1(forespoerselService = services.forespoerselService)
            sykmeldingV1(sykmeldingService = services.sykmeldingService)
            soeknadV1(soeknadService = services.soeknadService)
        }
    }
}
