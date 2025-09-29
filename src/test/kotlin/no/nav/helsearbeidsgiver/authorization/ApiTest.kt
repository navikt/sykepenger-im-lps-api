package no.nav.helsearbeidsgiver.authorization

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.config.getPdpService
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll

abstract class ApiTest {
    val orgnrUtenPdpTilgang = Orgnr.genererGyldig().verdi
    val hovedenhetOrgnrMedPdpTilgang = Orgnr.genererGyldig().verdi
    val underenhetOrgnrMedPdpTilgang = Orgnr.genererGyldig().verdi

    val repositories: Repositories = mockk<Repositories>(relaxed = true)
    val services: Services =
        configureServices(
            repositories = repositories,
            authClient = mockk(),
            unleashFeatureToggles = mockk(),
            database = mockk(),
            pdlService = mockk(),
        )

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

    fun mockPdpTilganger() {
        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")

        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnumre = match { it.contains(orgnrUtenPdpTilgang) },
                ressurs = any(),
            )
        } returns false

        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnumre =
                    match {
                        (it.contains(hovedenhetOrgnrMedPdpTilgang) || it.contains(underenhetOrgnrMedPdpTilgang)) &&
                            !it.contains(orgnrUtenPdpTilgang)
                    },
                ressurs = any(),
            )
        } returns true
    }
}
