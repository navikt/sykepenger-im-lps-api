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
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS

val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

private val apiRequestsTeller =
    Counter
        .builder("lpsapi_http_requests")
        .description("Teller antall http requests til LPS API")
        .withRegistry(registry)

private val dokumentHentetTeller =
    Counter
        .builder("lpsapi_dokument_hentet")
        .description("Teller antall dokumenter hentet, feks inntektsmelding, sykmelding, sykepengesoeknad, forespoersel")
        .withRegistry(registry)

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

enum class MetrikkDokumentType(
    val value: String,
) {
    INNTEKTSMELDING("inntektsmelding"),
    SYKMELDING("sykmelding"),
    SYKEPENGESOEKNAD("sykepengesoeknad"),
    FORESPOERSEL("forespoersel"),
}

internal fun tellDokumenterHentet(
    orgnr: String,
    dokumentType: MetrikkDokumentType,
    antall: Int = 1,
) = dokumentHentetTeller
    .withTags("orgnr" to orgnr, "dokumentType" to dokumentType.value)
    .increment(minOf(antall, MAX_ANTALL_I_RESPONS).toDouble())

private fun Meter.MeterProvider<Counter>.withTags(vararg tags: Pair<String, String>): Counter =
    this.withTags(tags.map { Tag.of(it.first, it.second) })
