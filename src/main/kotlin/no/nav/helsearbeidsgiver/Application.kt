package no.nav.helsearbeidsgiver

import com.nimbusds.jose.util.DefaultResourceRetriever
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.helsearbeidsgiver.auth.gyldigSupplierOgConsumer
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.inntektsmelding.MottakRepository
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.SimbaKafkaConsumer
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.plugins.configureRouting
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_CONNECT_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_READ_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_SIZE_LIMIT
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain
        .main(args)

@Suppress("unused")
fun Application.module() {
    val db = Database.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val mottakRepository = MottakRepository(db)
    val forespoerselService = ForespoerselService(forespoerselRepository)
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
    val kafka = Env.getProperty("kafkaConsumer.enabled").toBoolean()
    if (kafka) {
        launch(Dispatchers.Default) {
            startKafkaConsumer(
                Env.getProperty("kafkaConsumer.inntektsmelding.topic"),
                SimbaKafkaConsumer(
                    inntektsmeldingRepository,
                    forespoerselRepository,
                    mottakRepository,
                ),
            )
        }
    }
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging)
    install(Authentication) {
        tokenValidationSupport(
            "validToken",
            config =
                TokenSupportConfig(
                    IssuerConfig(
                        "maskinporten",
                        Env.getProperty("maskinporten.wellknownUrl"),
                        listOf(Env.getProperty("maskinporten.scopes")),
                        listOf("aud", "sub"),
                    ),
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
    }
    configureRouting(forespoerselService, inntektsmeldingService)
}
