package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun createHttpClient() =
    HttpClient(Apache5) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

class ApiFeil(
    val code: HttpStatusCode,
    val feilMelding: String,
) : Exception(feilMelding)

suspend fun PipelineContext<Unit, ApplicationCall>.fangFeil(
    melding: String,
    block: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit,
) {
    try {
        this.block()
    } catch (e: ApiFeil) {
        sikkerLogger().error(e.feilMelding, e)
        call.respond(e.code, e.feilMelding)
    } catch (e: Exception) {
        sikkerLogger().error(melding, e)
        call.respond(InternalServerError, melding)
    }
}
