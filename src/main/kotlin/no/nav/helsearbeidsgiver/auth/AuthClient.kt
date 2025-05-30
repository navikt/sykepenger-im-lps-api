package no.nav.helsearbeidsgiver.felles.auth

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.pipe.orDefault

interface AuthClient {
    fun tokenGetter(
        identityProvider: AuthClientIdentityProvider,
        target: String,
    ): () -> String

    suspend fun altinnExchange(maskinportenTokenGetter: String): String
}

class NoOpAuthClient : AuthClient {
    override fun tokenGetter(
        identityProvider: AuthClientIdentityProvider,
        target: String,
    ): () -> String = { "dummy-token" }

    override suspend fun altinnExchange(maskinportenTokenGetter: String): String = "dummy-token"
}

class DefaultAuthClient : AuthClient {
    private val sikkerLogger = sikkerLogger()
    private val httpClient = createHttpClient()

    private val tokenEndpoint = Env.getPropertyOrNull("NAIS_TOKEN_ENDPOINT").orDefault { "" }
    private val tokenExchangeEndpoint = Env.getPropertyOrNull("NAIS_TOKEN_EXCHANGE_ENDPOINT").orDefault { "" }
    private val tokenIntrospectionEndpoint = Env.getPropertyOrNull("NAIS_TOKEN_INTROSPECTION_ENDPOINT").orDefault { "" }

    override fun tokenGetter(
        identityProvider: AuthClientIdentityProvider,
        target: String,
    ): () -> String =
        {
            runBlocking {
                token(identityProvider, target).accessToken
            }
        }

    internal suspend fun token(
        provider: AuthClientIdentityProvider,
        target: String,
    ): TokenResponse =
        try {
            httpClient
                .submitForm(
                    url = tokenEndpoint,
                    formParameters =
                        parameters {
                            identityProvider(provider)
                            target(target)
                        },
                ).body()
        } catch (e: ResponseException) {
            e.logAndRethrow()
        }

    internal suspend fun exchange(
        provider: AuthClientIdentityProvider,
        target: String,
        userToken: String,
    ): TokenResponse =
        try {
            httpClient
                .submitForm(
                    url = tokenExchangeEndpoint,
                    formParameters =
                        parameters {
                            identityProvider(provider)
                            target(target)
                            userToken(userToken)
                        },
                ).body()
        } catch (e: ResponseException) {
            e.logAndRethrow()
        }

    internal suspend fun introspect(
        provider: AuthClientIdentityProvider,
        accessToken: String,
    ): TokenIntrospectionResponse =
        httpClient
            .submitForm(
                url = tokenIntrospectionEndpoint,
                formParameters =
                    parameters {
                        identityProvider(provider)
                        token(accessToken)
                    },
            ).body()

    override suspend fun altinnExchange(token: String): String {
        val tokenAltinn3ExchangeEndpoint =
            "${Env.getProperty("ALTINN_3_BASE_URL")}/authentication/api/v1/exchange/maskinporten"

        return httpClient
            .get(tokenAltinn3ExchangeEndpoint) {
                bearerAuth(token)
            }.bodyAsText()
            .replace("\"", "")
    }

    private suspend fun ResponseException.logAndRethrow(): Nothing {
        val error = response.body<ErrorResponse>()
        val msg = "Klarte ikke hente token. Feilet med status '${response.status}' og feilmelding '${error.errorDescription}'."

        sikkerLogger.error(msg)
        throw this
    }
}
