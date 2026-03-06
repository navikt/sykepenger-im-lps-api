package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

fun createHttpClient() =
    HttpClient(Apache5) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 5000
        }
    }

fun String.toUuidOrNull() = runCatching(UUID::fromString).getOrNull()

fun LocalDate.tilTidspunktStartOfDay(): LocalDateTime = LocalDateTime.of(this, LocalTime.MIN)

fun LocalDate.tilTidspunktEndOfDay(): LocalDateTime = LocalDateTime.of(LocalDate.of(this.year, this.month, this.dayOfMonth), LocalTime.MAX)
