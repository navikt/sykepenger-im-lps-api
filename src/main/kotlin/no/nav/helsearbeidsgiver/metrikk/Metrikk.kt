package no.nav.helsearbeidsgiver.metrikk

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.RoutingContext
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.plugins.skalLoggeMetrikk

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

val metrikkPlugin =
    createApplicationPlugin(name = "MetrikkPlugin") {
        on(ResponseSent) { call ->
            if (call.attributes.getOrNull(skalLoggeMetrikk) != null) {
                runBlocking { call.tellApiRequest() }
            }
        }
    }

private suspend fun ApplicationCall.tellApiRequest() {
    val metode = request.httpMethod.value
    val path = request.path()
    val ressurs = if (metode == "GET") path.substringBeforeLast("/") else path
    val responseKode = response.status()?.value.toString()
    apiRequestsTeller
        .withTags(
            "orgnr" to tokenValidationContext().getConsumerOrgnr(),
            "ressurs" to ressurs,
            "metode" to metode,
            "respons" to responseKode,
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
