package helsearbeidsgiver.nav.no

import com.nimbusds.jose.util.DefaultResourceRetriever
import helsearbeidsgiver.nav.no.plugins.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.auth.Authentication
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_CONNECT_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_READ_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_SIZE_LIMIT
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Authentication) {
        tokenValidationSupport(
            "validToken",
            config = TokenSupportConfig(
                IssuerConfig(
                   "maskinporten",
                   System.getenv("MASKINPORTEN_WELL_KNOWN_URL"),
                   listOf(System.getenv("MASKINPORTEN_SCOPES")),
                    listOf("aud", "sub")
                ),
//            ),
            //local:
//                IssuerConfig(
//                    "iss-localhost",
//                   "http://localhost:33445/default/.well-known/openid-configuration",
//                    listOf("aud-localhost, nav:inntektsmelding/lps.write"),
//                    listOf("aud", "sub")
//                ),
//                IssuerConfig(
//                    "maskinporten-test",
//                    "https://test.maskinporten.no/.well-known/oauth-authorization-server",
//                    listOf("nav:inntektsmelding/lps.write"),
//                    listOf("aud", "sub")
//                )
            ),
            resourceRetriever = DefaultResourceRetriever(
                DEFAULT_HTTP_CONNECT_TIMEOUT,
                DEFAULT_HTTP_READ_TIMEOUT,
                DEFAULT_HTTP_SIZE_LIMIT
            )
        )
        // Configure authentication
    }
    configureRouting()
}
