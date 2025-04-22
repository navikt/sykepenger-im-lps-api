package no.nav.helsearbeidsgiver.authorization

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.mockk
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll

abstract class ApiTest {
    val repositories: Repositories = mockk<Repositories>(relaxed = true)
    val services: Services = configureServices(repositories = repositories, authClient = mockk())

    private val port = 33445
    val mockOAuth2Server: MockOAuth2Server =
        MockOAuth2Server().apply {
            start(port = port)
        }
    private val testApplication: TestApplication =
        TestApplication {
            application {
                apiModule(services = services, authClient = mockk())
            }
        }
    val client: HttpClient =
        testApplication.createClient {
            install(ContentNegotiation) {
                json()
            }
        }

    @AfterAll
    fun shutdownStuff() {
        testApplication.stop()
        mockOAuth2Server.shutdown()
    }
}
