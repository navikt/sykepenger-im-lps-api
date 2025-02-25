package no.nav.helsearbeidsgiver

import com.nimbusds.jose.util.DefaultResourceRetriever
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.helsearbeidsgiver.Env.getProperty
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.auth.AltinnAuthClient
import no.nav.helsearbeidsgiver.auth.gyldigScope
import no.nav.helsearbeidsgiver.auth.gyldigSystembrukerOgConsumer
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.dialogporten.IngenDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.innsending.InnsendingRepository
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.createKafkaConsumerConfig
import no.nav.helsearbeidsgiver.kafka.createKafkaProducerConfig
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingSerializer
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.pdp.IPdpService
import no.nav.helsearbeidsgiver.pdp.IngenTilgangPdpService
import no.nav.helsearbeidsgiver.pdp.LocalhostPdpService
import no.nav.helsearbeidsgiver.pdp.PdpService
import no.nav.helsearbeidsgiver.pdp.lagPdpClient
import no.nav.helsearbeidsgiver.plugins.configureRouting
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_CONNECT_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_READ_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_SIZE_LIMIT
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer

fun main() {
    startServer()
}

fun startServer() {
    val authClient = AltinnAuthClient()
    val pdpService =
        when {
            isDev() -> PdpService(lagPdpClient(authClient))
            isLocal() -> LocalhostPdpService()
            else -> IngenTilgangPdpService()
        }
    // val dialogService = if (isDev()) DialogportenService(lagDialogportenClient(authClient)) else IngenDialogportenService()
    val dialogService = IngenDialogportenService()

    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            apiModule(
                pdpService = pdpService,
                dialogportenService = dialogService,
            )
        },
    ).start(wait = true)
}

@Suppress("unused")
fun Application.apiModule(
    pdpService: IPdpService,
    dialogportenService: IDialogportenService,
) {
    sikkerLogger().info("Starter applikasjon!")

    val db = Database.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val mottakRepository = MottakRepository(db)
    val innsendingRepository = InnsendingRepository(db)

    val forespoerselService = ForespoerselService(forespoerselRepository)
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)

    val innsendingProducer =
        InnsendingProducer(
            KafkaProducer(
                createKafkaProducerConfig(producerName = "api-innsending-producer"),
                StringSerializer(),
                InnsendingSerializer(),
            ),
        )

    val innsendingService =
        InnsendingService(
            innsendingProducer = innsendingProducer,
            innsendingRepository = innsendingRepository,
        )

    val inntektsmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("im"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            getProperty("kafkaConsumer.inntektsmelding.topic"),
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
            getProperty("kafkaConsumer.forespoersel.topic"),
            forespoerselKafkaConsumer,
            ForespoerselTolker(
                forespoerselRepository,
                mottakRepository,
                dialogportenService,
            ),
        )
    }

    install(ContentNegotiation) {
        json()
    }
    install(Authentication) {
        tokenValidationSupport(
            name = "systembruker-config",
            config =
                TokenSupportConfig(
                    IssuerConfig(
                        name = "maskinporten",
                        discoveryUrl = getProperty("maskinporten.wellknownUrl"),
                        acceptedAudience = listOf(getProperty("maskinporten.eksponert_scopes")),
                        optionalClaims = listOf("aud", "sub"),
                    ),
                ),
            requiredClaims =
                RequiredClaims(
                    issuer = "maskinporten",
                    claimMap = arrayOf("authorization_details", "consumer", "scope"),
                ),
            additionalValidation = {
                it.gyldigScope() && it.gyldigSystembrukerOgConsumer(pdpService)
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
    configureRouting(forespoerselService, inntektsmeldingService, innsendingService)
}

private fun isDev(): Boolean = "dev-gcp".equals(getPropertyOrNull("NAIS_CLUSTER_NAME"), true)

private fun isLocal(): Boolean = "localhost".equals(getPropertyOrNull("APP_ENV"), true)
