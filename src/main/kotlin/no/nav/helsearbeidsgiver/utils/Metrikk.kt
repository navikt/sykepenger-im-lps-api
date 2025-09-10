package no.nav.helsearbeidsgiver.utils

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

private val sykmeldingTeller =
    Counter
        .builder("lpsapi_sykmeldinger_hentet_test1")
        .description("Teller antall sykmeldinger hentet")
        .withRegistry(registry)

internal fun tellSykmeldingHentet(orgnr: String) =
    sykmeldingTeller
        .withTag("orgnr", orgnr)
        .increment()
