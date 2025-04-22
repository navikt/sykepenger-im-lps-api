package no.nav.helsearbeidsgiver.felles.auth

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ParametersBuilder
import io.ktor.serialization.kotlinx.json.json
import no.nav.helsearbeidsgiver.utils.json.jsonConfig

internal fun createHttpClient(): HttpClient = HttpClient(Apache5) { configure() }

internal fun HttpClientConfig<*>.configure() {
    expectSuccess = true

    install(ContentNegotiation) {
        json(jsonConfig)
    }
}

internal fun ParametersBuilder.identityProvider(identityProvider: AuthClientIdentityProvider) {
    append("identity_provider", identityProvider.verdi)
}

internal fun ParametersBuilder.target(target: String) {
    append("target", target)
}

internal fun ParametersBuilder.userToken(userToken: String) {
    append("user_token", userToken)
}

internal fun ParametersBuilder.token(token: String) {
    append("token", token)
}
