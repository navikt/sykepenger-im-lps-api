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
import no.nav.helsearbeidsgiver.utils.LeaderConfig
import no.nav.helsearbeidsgiver.utils.TIGERSYS_ORGNR
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

@WithKafkaContainer
@WithPostgresContainer
abstract class LpsApiIntegrasjontest {
    lateinit var db: Database
    lateinit var repositories: Repositories
    lateinit var services: Services
    lateinit var tolkers: Tolkere
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")

    val mockUnleash = mockk<UnleashFeatureToggles>(relaxed = true)
    val mockLeaderConfig = mockk<LeaderConfig>(relaxed = true)

    val server =
        embeddedServer(
            factory = Netty,
            port = 8080,
            module = {
                apiModule(services = services, authClient = mockk(relaxed = true), unleashFeatureToggles = mockUnleash)
                configureKafkaConsumers(tolkers, mockUnleash, leaderConfig = mockLeaderConfig)
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
        every { mockLeaderConfig.isElectedLeader() } returns false
        every { mockUnleash.skalKonsumereSykepengesoeknader() } returns true
        every { mockUnleash.skalKonsumereForespoersler() } returns true
        every { mockUnleash.skalKonsumereInntektsmeldinger() } returns true
        every { mockUnleash.skalEksponereSykepengesoeknader() } returns true
        every { mockUnleash.skalEksponereForespoersler() } returns true
        every { mockUnleash.skalEksponereInntektsmeldinger() } returns true
        every { mockUnleash.skalEksponereSykmeldinger(TIGERSYS_ORGNR) } returns true
        every { mockUnleash.skalKonsumereStatusISpeil() } returns true
        every { mockUnleash.skalKonsumereAvvisteInntektsmeldinger() } returns true
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        repositories = configureRepositories(db)
        services =
            configureServices(
                repositories = repositories,
                unleashFeatureToggles = mockUnleash,
                database = db,
                pdlService = mockk(),
                leaderConfig = mockLeaderConfig,
            )
        tolkers =
            configureTolkere(
                services = services,
                repositories = repositories,
            )

        server.start(wait = false)
    }

    @BeforeEach
    fun cleanUp() {
        transaction(db) {
            InntektsmeldingEntitet.deleteAll()
            ForespoerselEntitet.deleteAll()
            SoeknadEntitet.deleteAll()
        }
        every { mockUnleash.skalEksponereForespoersler() } returns true
        every { mockUnleash.skalEksponereInntektsmeldinger() } returns true
    }

    @AfterAll
    fun tearDown() {
        client.close()
        mockOAuth2Server.shutdown()
        server.stop()
    }

    /**
     * `betingelse` er en suspenderende lambda som får HTTP-responsen og skal returnere `true`
     * når ønsket tilstand er oppnådd. Retry-løkken stopper tidlig hvis responsen har status 200
     * og `betingelse` returnerer `true`. Bruk denne for å vente på en spesifikk tilstand i responsen.
     */
    suspend fun fetchWithRetry(
        url: String,
        token: String,
        betingelse: suspend (HttpResponse) -> Boolean = {
            true
        },
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
            if (response.status.value == 200 && betingelse(response)) break
            delay(delayMillis)
        } while (attempts < maxAttempts)

        return response
    }
}
