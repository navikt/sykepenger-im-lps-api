package no.nav.helsearbeidsgiver.testcontainer

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.Tolkere
import no.nav.helsearbeidsgiver.config.configureKafkaConsumers
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.config.configureTolkere
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.time.Duration
import java.util.Properties

@WithKafkaContainer
@WithPostgresContainer
abstract class LpsApiIntegrasjontest {
    lateinit var db: Database
    lateinit var repositories: Repositories
    lateinit var services: Services
    lateinit var tolkers: Tolkere
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")

    val mockUnleash = mockk<UnleashFeatureToggles>(relaxed = true)
    val server =
        embeddedServer(
            factory = Netty,
            port = 8080,
            module = {
                apiModule(services = services, authClient = mockk(relaxed = true))
                configureKafkaConsumers(tolkers, mockUnleash)
            },
        )
    val mockOAuth2Server =
        MockOAuth2Server().apply {
            start(port = 33445)
        }
    val client =
        HttpClient {
            install(ContentNegotiation) {
                json()
            }
        }

    @BeforeAll
    fun setup() {
        every { mockUnleash.skalKonsumereSykepengesoeknader() } returns true
        every { mockUnleash.skalKonsumereStatusISpeil() } returns true
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        repositories = configureRepositories(db)
        services = configureServices(repositories, mockk(relaxed = true), mockk(relaxed = true), db)
        tolkers = configureTolkere(services, repositories)

        server.start(wait = false)

        val dummyProps =
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("KAFKA_BOOTSTRAP_SERVERS"))
                put(ConsumerConfig.GROUP_ID_CONFIG, "test-wait-group")
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            }
        val dummyConsumer = KafkaConsumer<String, String>(dummyProps)
        dummyConsumer.subscribe(listOf("helsearbeidsgiver.pri")) // One of your topics

        runBlocking {
            // Poll until assigned (use a loop with timeout)
            var assigned = false
            repeat(30) {
                // 30s max
                val records = dummyConsumer.poll(Duration.ofSeconds(1))
                if (dummyConsumer.assignment().isNotEmpty()) {
                    assigned = true
                    return@repeat
                }
                delay(1000)
            }
            if (!assigned) throw AssertionError("Kafka consumers not ready")
        }
        dummyConsumer.close()
    }

    @BeforeEach
    fun cleanUp() {
        transaction(db) {
            InntektsmeldingEntitet.deleteAll()
            ForespoerselEntitet.deleteAll()
            SoeknadEntitet.deleteAll()
        }
    }

    @AfterAll
    fun tearDown() {
        client.close()
        mockOAuth2Server.shutdown()
        server.stop()
    }

    suspend fun fetchWithRetry(
        url: String,
        token: String,
        maxAttempts: Int = 5,
        delayMillis: Long = 100L,
    ): HttpResponse {
        var attempts = 0
        lateinit var response: HttpResponse

        do {
            attempts++
            response =
                client.get(url) {
                    bearerAuth(token)
                }
            if (response.status.value == 200) break
            delay(delayMillis)
        } while (attempts < maxAttempts)

        return response
    }
}
