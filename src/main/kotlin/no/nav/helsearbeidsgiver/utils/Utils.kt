package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import java.util.UUID

fun createHttpClient() =
    HttpClient(Apache5) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

fun String.toUuidOrNull() = runCatching(UUID::fromString).getOrNull()

class ApiFeil(
    val code: HttpStatusCode,
    val feilMelding: String,
) : Exception(feilMelding)

// TODO: Denne lekte ikke så bra med ktor3. Fjerner for nå, enable igjen?
// suspend fun RoutingContext.fangFeil(
//    melding: String,
//    block: RoutingContext.() -> Unit,
// ) {
//    try {
//        this.block()
//    } catch (e: ApiFeil) {
//         sikkerLogger().error(e.feilMelding, e)
//        call.respond(e.code, e.feilMelding)
//    } catch (e: Exception) {
//        sikkerLogger().error(melding, e)
//        call.respond(InternalServerError, melding)
//    }
// }
