package no.nav.helsearbeidsgiver

import com.nimbusds.jose.util.DefaultResourceRetriever
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.helsearbeidsgiver.auth.gyldigSupplierOgConsumer
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.createKafkaConsumerConfig
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.plugins.configureRouting
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_CONNECT_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_READ_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_SIZE_LIMIT
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import org.apache.kafka.clients.consumer.KafkaConsumer

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain
        .main(args)

@Suppress("unused")
fun Application.module() {
    sikkerLogger().info("Starter applikasjon!")
    val db = Database.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val mottakRepository = MottakRepository(db)
    val forespoerselService = ForespoerselService(forespoerselRepository)
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
    val kafka = Env.getProperty("kafkaConsumer.enabled").toBoolean()
    if (kafka) {
        val inntektsmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("im"))
        launch(Dispatchers.Default) {
            startKafkaConsumer(
                Env.getProperty("kafkaConsumer.inntektsmelding.topic"),
                inntektsmeldingKafkaConsumer,
                InntektsmeldingTolker(
                    inntektsmeldingService,
                    mottakRepository,
                ),
            )
        }
        val forespoerselKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("fsp"))
        launch(Dispatchers.Default) {
            startKafkaConsumer(
                Env.getProperty("kafkaConsumer.forespoersel.topic"),
                forespoerselKafkaConsumer,
                ForespoerselTolker(
                    forespoerselRepository,
                    mottakRepository,
                ),
            )
        }
    }
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
                        Env.getProperty("maskinporten.wellknownUrl"),
                        listOf(Env.getProperty("maskinporten.eksponert_scopes")),
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
    sikkerLogger().info("Setter opp ruting...")
    configureRouting(forespoerselService, inntektsmeldingService)
}
