package no.nav.helsearbeidsgiver.utils

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider

fun setupOpenTelemetryWithPrometheus(): Meter {
    // Create Prometheus HTTP server (default port 9464)
    val prometheusServer = PrometheusHttpServer.create()

    // Initialize SDK MeterProvider with Prometheus exporter
    val meterProvider =
        SdkMeterProvider
            .builder()
            .registerMetricReader(prometheusServer)
            .build()

    val openTelemetry =
        OpenTelemetrySdk
            .builder()
            .setMeterProvider(meterProvider)
            .buildAndRegisterGlobal()

    return openTelemetry.getMeter("sykepenger-lps-api")
}

fun incrementCounter(
    counter: LongCounter,
    labels: Map<String, String>,
) {
    counter.add(
        1,
        io.opentelemetry.api.common.Attributes
            .builder()
            .apply {
                labels.forEach { (key, value) -> put(key, value) }
            }.build(),
    )
}

val meter = setupOpenTelemetryWithPrometheus()

// Example: Create a counter
val sykmeldingCounter =
    meter
        .counterBuilder("lpsapi_sykmeldinger_hentet_test1")
        .setDescription("Totalt antall sykmeldinger hentet")
        .build()
