package no.nav.helsearbeidsgiver.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.utils.json.jsonConfig

class AltinnAuthClient {
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
                json(jsonConfig)
            }
        }

    fun getToken(): String =
        runBlocking {
            val texasTokenEndpoint = Env.getProperty("NAIS_TOKEN_ENDPOINT")
            val altinn3Url = Env.getProperty("ALTINN_3_URL")
            val altinnPdpScope = Env.getProperty("ALTINN_PDP_SCOPE")
            val maskinportenToken: String =
                httpClient
                    .post(texasTokenEndpoint) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        setBody(
                            listOf(
                                "identity_provider" to "maskinporten",
                                "target" to altinnPdpScope,
                            ).formUrlEncode(),
                        )
                    }.body<TokenResponse>()
                    .accessToken
            val altinnToken: String =
                httpClient
                    .get("$altinn3Url/authentication/api/v1/exchange/maskinporten") {
                        bearerAuth(maskinportenToken)
                    }.bodyAsText()
                    .replace("\"", "")
            altinnToken
        }
}
