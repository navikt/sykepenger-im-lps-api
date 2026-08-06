package no.nav.helsearbeidsgiver.plugins

import io.ktor.http.ContentType
import io.ktor.openapi.KotlinxSerializerDefaultFormats
import io.ktor.openapi.KotlinxSerializerJsonSchemaInference
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.plugin
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.OperationDescribeAttributeKey
import io.ktor.server.routing.openapi.registerBearerAuthSecurityScheme
import io.ktor.server.routing.routing
import kotlinx.serialization.modules.EmptySerializersModule
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.forespoersel.forespoerselV1
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.inntekt.inntektV1
import no.nav.helsearbeidsgiver.inntektsmelding.inntektsmeldingV1
import no.nav.helsearbeidsgiver.metrikk.metrikkRoutes
import no.nav.helsearbeidsgiver.soeknad.soeknadTokenX
import no.nav.helsearbeidsgiver.soeknad.soeknadV1
import no.nav.helsearbeidsgiver.sykmelding.sykmeldingTokenX
import no.nav.helsearbeidsgiver.sykmelding.sykmeldingV1
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.util.ArrayDeque

fun Application.configureRouting(
    services: Services,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    routing {
        metrikkRoutes()
        naisRoutes(services.helseSjekkService)
        authenticate("systembruker-config") {
            inntektsmeldingV1(
                services = services,
                unleashFeatureToggles = unleashFeatureToggles,
            )
            inntektV1(services)
            forespoerselV1(
                forespoerselService = services.forespoerselService,
                unleashFeatureToggles = unleashFeatureToggles,
            )
            sykmeldingV1(sykmeldingService = services.sykmeldingService, unleashFeatureToggles)
            soeknadV1(soeknadService = services.soeknadService, unleashFeatureToggles)
        }
        authenticate("tokenx-config") {
            sykmeldingTokenX(sykmeldingService = services.sykmeldingService)
            soeknadTokenX(soeknadService = services.soeknadService)
        }
        swaggerUI(path = "swagger") {
            info =
                OpenApiInfo(
                    title = "Sykepenger API",
                    version = "1.0.0",
                    description = "API for sykmelding, sykepengesøknad og inntektsmelding for sykepenger",
                )
            registerBearerAuthSecurityScheme("systembruker-config")
            remotePath = "documentation.yaml"
            source = openApiRoutingSource()
        }
    }
}

private fun openApiRoutingSource(): OpenApiDocSource =
    OpenApiDocSource.Routing(
        contentType = ContentType.Application.Yaml,
        schemaInference =
            KotlinxSerializerJsonSchemaInference(
                module = EmptySerializersModule(),
                formats = { descriptor ->
                    KotlinxSerializerDefaultFormats(descriptor)
                        ?: when (descriptor.serialName.removeSuffix("?")) {
                            LocalDateSerializer.descriptor.serialName -> {
                                "date"
                            }

                            LocalDateTimeSerializer.descriptor.serialName -> {
                                "date-time"
                            }

                            UuidSerializer.descriptor.serialName -> {
                                "uuid"
                            }

                            else -> {
                                null
                            }
                        }
                },
            ),
        routes = {
            plugin(RoutingRoot.Plugin)
                .allRoutes()
                .filter { it.attributes.contains(OperationDescribeAttributeKey) }
        },
    )

private fun Route.allRoutes(): Sequence<Route> {
    val stack = ArrayDeque<Route>()
    stack.addLast(this)

    return sequence {
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            yield(current)
            current.children.forEach(stack::addLast)
        }
    }
}

suspend inline fun <reified T> ApplicationCall.respondWithMaxLimit(entities: List<T>) {
    if (entities.size > MAX_ANTALL_I_RESPONS) {
        response.header("X-Warning-limit-reached", MAX_ANTALL_I_RESPONS)
        val liste = entities.subList(0, MAX_ANTALL_I_RESPONS)
        respond(liste)
    } else {
        respond(entities)
    }
}
