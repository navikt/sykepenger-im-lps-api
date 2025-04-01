package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class ForespoerselIT {
    private val db: Database
    private val repositories: Repositories
    private val services: Services

    private val port = 33445
    private val mockOAuth2Server: MockOAuth2Server
    private val testApplication: TestApplication
    private val client: HttpClient
    private val forespoerselTolker: ForespoerselTolker
    private val dialogportenService: DialogportenService

    init {
        db = DbConfig.init()
        repositories = configureRepositories(db)
        services = configureServices(repositories)
        dialogportenService = mockk(relaxed = true)
        forespoerselTolker =
            ForespoerselTolker(
                forespoerselRepository = repositories.forespoerselRepository,
                mottakRepository = repositories.mottakRepository,
                dialogportenService = dialogportenService,
            )
        mockOAuth2Server =
            MockOAuth2Server().apply {
                start(port = port)
            }
        testApplication =
            TestApplication {
                application {
                    apiModule(services = services)
                }
            }
        client =
            testApplication.createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
    }

    @Test
    fun `les forespoersel på kafka og les gjennom apiet`() {
        runTest {
            forespoerselTolker.lesMelding(
                TestData.FORESPOERSEL_MOTTATT,
            )
            val orgnr1 = "810007982"
            verify { dialogportenService.opprettDialog(any(), any()) }

            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr1))
                }
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<ForespoerselResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.forespoersler[0].status shouldBe Status.AKTIV
            forespoerselSvar.forespoersler[0].orgnr shouldBe orgnr1
        }
    }

    @AfterAll
    fun shutdownStuff() {
        testApplication.stop()
        mockOAuth2Server.shutdown()
    }
}
