package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class InnsendingIT {
    private val db: Database = DbConfig.init()
    private val repositories: Repositories = configureRepositories(db)
    private val services: Services = configureServices(repositories)

    private val port = 33445
    private val mockOAuth2Server =
        MockOAuth2Server().apply {
            start(port = port)
        }
    private val testApplication =
        TestApplication {
            application {
                apiModule(services = services)
            }
        }
    private val client =
        testApplication.createClient {
            install(ContentNegotiation) {
                json()
            }
        }
    val inntektsmeldingTolker = InntektsmeldingTolker(services.inntektsmeldingService, repositories.mottakRepository)

    @Test
    fun `les inntektsmelding på kafka og hent gjennom apiet`() {
        runTest {
            inntektsmeldingTolker.lesMelding(
                TestData.IM_MOTTATT,
            )
            val orgnr1 = "810007982"
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr1))
                }
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<InntektsmeldingFilterResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.inntektsmeldinger[0].status shouldBe InnsendingStatus.GODKJENT
            forespoerselSvar.inntektsmeldinger[0].arbeidsgiver.orgnr shouldBe orgnr1
        }
    }

    @AfterAll
    fun shutdownStuff() {
        testApplication.stop()
        mockOAuth2Server.shutdown()
    }
}
