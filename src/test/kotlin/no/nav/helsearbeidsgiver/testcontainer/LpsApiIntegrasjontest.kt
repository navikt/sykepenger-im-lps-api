package no.nav.helsearbeidsgiver.testcontainer

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.mockk.mockk
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.Tolkere
import no.nav.helsearbeidsgiver.config.configureKafkaConsumers
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.config.configureTolkere
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

@WithKafkaContainer
@WithPostgresContainer
abstract class LpsApiIntegrasjontest {
    lateinit var db: Database
    lateinit var repositories: Repositories
    lateinit var services: Services
    lateinit var tolkers: Tolkere

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
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        repositories = configureRepositories(db)
        services = configureServices(repositories, mockk(relaxed = true))
        tolkers = configureTolkere(services, repositories)

        embeddedServer(
            factory = Netty,
            port = 8080,
            module = {
                apiModule(services = services, authClient = mockk(relaxed = true))
                configureKafkaConsumers(tolkers)
            },
        ).start(wait = false)
    }

    @AfterAll
    fun tearDown() {
        client.close()
        mockOAuth2Server.shutdown()
    }
}
