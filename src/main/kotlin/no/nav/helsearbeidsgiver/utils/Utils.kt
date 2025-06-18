package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun createHttpClient() =
    HttpClient(Apache5) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

fun String.toUuidOrNull() = runCatching(UUID::fromString).getOrNull()

fun LocalDate.tilTidspunktStartOfDay(): LocalDateTime = LocalDateTime.of(this.year, this.month, this.dayOfMonth, 0, 0)

fun LocalDate.tilTidspunktEndOfDay(): LocalDateTime = LocalDateTime.of(this.year, this.month, this.dayOfMonth, 23, 59, 59, 999999999)

class ApiFeil(
    val code: HttpStatusCode,
    val feilMelding: String,
) : Exception(feilMelding)

suspend fun RoutingContext.fangFeil(
    melding: String,
    block: suspend RoutingContext.() -> Unit,
) {
    try {
        block()
    } catch (e: ApiFeil) {
        sikkerLogger().error(e.feilMelding, e)
        call.respond(e.code, e.feilMelding)
    } catch (e: Exception) {
        sikkerLogger().error(melding, e)
        call.respond(InternalServerError, melding)
    }
}
