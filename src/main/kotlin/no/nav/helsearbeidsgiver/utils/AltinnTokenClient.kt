package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

class AltinnTokenClient {
    val httpClient = HttpClient(Apache5)

    fun getToken(): String =
        runBlocking {
            val maskinportenToken =
                httpClient
                    .post(System.getenv("NAIS_TOKEN_ENDPOINT")) {
                        header("Content-Type", "application/json")
                        header("Accept", "application/json")
                        setBody(
                            mapOf(
                                "identity_provider" to "maskinporten",
                                "target" to "altinn:authorization/authorize",
                            ),
                        )
                    }.body<Map<String, Any>>()["access_token"] as String
            httpClient
                .get("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten") {
                    header("Authorization", "Bearer $maskinportenToken")
                }.bodyAsText()
        }
}
