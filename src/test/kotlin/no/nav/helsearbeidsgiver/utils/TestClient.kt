package no.nav.helsearbeidsgiver.utils

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.utils.json.toJson

class TestClient(
    appTestBuilder: ApplicationTestBuilder,
    private val authToken: () -> String,
) {
    private val httpClient =
        appTestBuilder.createClient {
            install(ContentNegotiation) {
                json()
            }
        }

    fun get(
        path: String,
        block: HttpRequestBuilder.() -> Unit = { withAuth() },
    ): HttpResponse =
        runBlocking {
            httpClient.get(path) {
                block()
            }
        }

    fun post(
        path: String,
        body: JsonElement,
        block: HttpRequestBuilder.() -> Unit = { withAuth() },
    ): HttpResponse =
        runBlocking {
            httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(body)

                block()
            }
        }

    fun <T : Any> post(
        path: String,
        body: T,
        bodySerializer: KSerializer<T>,
        block: HttpRequestBuilder.() -> Unit = { withAuth() },
    ): HttpResponse =
        post(
            path,
            body.toJson(bodySerializer),
            block,
        )

    private fun HttpRequestBuilder.withAuth() {
        val token = authToken()
        bearerAuth(token)
    }
}
