package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class AltinnTokenClient {
    @Serializable
    data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("token_type") val tokenType: String,
        @SerialName("expires_in") val expiresInSeconds: Long,
    )

    val httpClient =
        HttpClient(Apache5) {
            expectSuccess = true
            install(ContentNegotiation) {
                Json
            }
        }

    fun getToken(): String =
        runBlocking {
            val maskinportenToken: String =
                httpClient
                    .post(System.getenv("NAIS_TOKEN_ENDPOINT")) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        accept(ContentType.Application.Json)
                        setBody(
                            listOf(
                                "identity_provider" to "maskinporten",
                                "target" to "altinn:authorization/authorize",
                            ).formUrlEncode(),
                        )
                    }.body<TokenResponse>()
                    .accessToken
            sikkerLogger().info("maskinportenToken: $maskinportenToken")
            val altinnToken: String =
                httpClient
                    .get("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten") {
                        header("Authorization", "Bearer $maskinportenToken")
                    }.body()
            sikkerLogger().info("altinnToken: $altinnToken")
            altinnToken
        }
}
