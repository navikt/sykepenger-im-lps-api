package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@WithPostgresContainer
class ForespoerselIT {
    private lateinit var db: Database
    private lateinit var repositories: Repositories
    private lateinit var services: Services
    private lateinit var forespoerselTolker: ForespoerselTolker
    private val authClient = mockk<AuthClient>(relaxed = true)

    private val orgnr = Orgnr("810007982")

    private val port = 33445
    private val mockOAuth2Server =
        MockOAuth2Server().apply {
            start(port = port)
        }
    private val testApplication =
        TestApplication {
            application {
                apiModule(services = services, authClient = authClient)
            }
        }
    private val client =
        testApplication.createClient {
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

        val unleashMock = mockk<UnleashFeatureToggles>()
        every { unleashMock.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns true

        services = configureServices(repositories, authClient, unleashMock, db)

        forespoerselTolker =
            ForespoerselTolker(
                mottakRepository = repositories.mottakRepository,
                dialogportenService = services.dialogportenService,
                forespoerselService = services.forespoerselService,
            )
    }

    @Test
    fun `les forespoersel på kafka og les gjennom apiet`() {
        forespoerselTolker.lesMelding(
            TestData.FORESPOERSEL_MOTTATT,
        )
        runBlocking {
            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr.toString()))
                }
            response.status.value shouldBe 200

            val forespoerselSvar = response.body<List<Forespoersel>>()

            forespoerselSvar.size shouldBe 1
            forespoerselSvar[0].status shouldBe Status.AKTIV
            forespoerselSvar[0].orgnr shouldBe orgnr.toString()
        }
    }

    @AfterAll
    fun shutdownStuff() {
        testApplication.stop()
        mockOAuth2Server.shutdown()
    }
}
