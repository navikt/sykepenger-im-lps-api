package no.nav.helsearbeidsgiver.metrikk

import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.RoutingContext
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext

val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

private val sykmeldingTeller =
    Counter
        .builder("lpsapi_sykmelding_hentet")
        .description("Teller antall sykmeldinger hentet")
        .withRegistry(registry)

private val apiRequestsTeller =
    Counter
        .builder("lpsapi_http_requests_test1")
        .description("Teller antall http requests til LPS API")
        .withRegistry(registry)

internal fun tellSykmeldingHentet(
    orgnr: String,
    antall: Int = 1,
) = sykmeldingTeller
    .withTags("orgnr" to orgnr)
    .increment(antall.toDouble())

suspend fun RoutingContext.tellApiRequest() {
    val metode = call.request.httpMethod.value
    val path = call.request.path()
    val ressurs = if (metode == "GET") path.substringBeforeLast("/") else path
    apiRequestsTeller
        .withTags(
            "orgnr" to tokenValidationContext().getConsumerOrgnr(),
            "ressurs" to ressurs,
            "metode" to metode,
        ).increment()
}

private fun Meter.MeterProvider<Counter>.withTags(vararg tags: Pair<String, String>): Counter =
    this.withTags(tags.map { Tag.of(it.first, it.second) })
