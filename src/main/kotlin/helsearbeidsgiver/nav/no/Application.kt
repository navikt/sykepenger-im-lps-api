package helsearbeidsgiver.nav.no

import com.nimbusds.jose.util.DefaultResourceRetriever
import helsearbeidsgiver.nav.no.kafka.startKafkaConsumer
import helsearbeidsgiver.nav.no.plugins.configureRouting
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.helsearbeidsgiver.db.DatabaseFactory
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_CONNECT_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_READ_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_SIZE_LIMIT
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain
        .main(args)

fun Application.module() {
    DatabaseFactory.init()

    startKafkaConsumer()
    install(ContentNegotiation) {
        json()
    }
    install(Authentication) {
        tokenValidationSupport(
            "validToken",
            config =
                TokenSupportConfig(
                    IssuerConfig(
                        "maskinporten",
                        Env.wellKnownUrl,
                        listOf(Env.scopes),
                        listOf("aud", "sub"),
                    ),
                    // local:
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
            additionalValidation = {
                it.gyldigSupplierOgConsumer()
            },
            resourceRetriever =
                DefaultResourceRetriever(
                    DEFAULT_HTTP_CONNECT_TIMEOUT,
                    DEFAULT_HTTP_READ_TIMEOUT,
                    DEFAULT_HTTP_SIZE_LIMIT,
                ),
        )
        // Configure authentication
    }
    configureRouting()
}

private fun TokenValidationContext.gyldigSupplierOgConsumer(): Boolean {
    val supplier = this.getClaims("maskinporten").get("supplier") as Map<String, String>
    val consumer = this.getClaims("maskinporten").get("consumer") as Map<String, String>
    val supplierOrgnr = supplier.extractOrgnummer()
    val consumerOrgnr = consumer.extractOrgnummer()
    return supplierOrgnr != null &&
        consumerOrgnr != null &&
        supplierOrgnr.matches(Regex("\\d{9}")) &&
        consumerOrgnr.matches(Regex("\\d{9}"))
}

private fun Map<String, String>.extractOrgnummer(): String? =
    get("ID")
        ?.split(":")
        ?.get(1)
