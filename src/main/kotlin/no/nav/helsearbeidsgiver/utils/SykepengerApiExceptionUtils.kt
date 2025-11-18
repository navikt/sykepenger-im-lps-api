package no.nav.helsearbeidsgiver.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive

sealed class SykepengerApiException(
    override val message: String,
) : Exception(message) {
    class NotFound(
        message: String,
    ) : SykepengerApiException(message)

    class Unauthorized(
        message: String,
    ) : SykepengerApiException(message)

    class BadRequest(
        message: String,
    ) : SykepengerApiException(message)
}

suspend inline fun <reified T : Any> ApplicationCall.receiveFilter(): T =
    try {
        receive<T>()
    } catch (_: ContentTransformationException) {
        throw SykepengerApiException.BadRequest("Request mangler eller har ugyldig body")
    } catch (_: BadRequestException) {
        throw SykepengerApiException.BadRequest("Ugyldig filterparameter")
    }
