package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.buildJournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@WithPostgresContainer
class InnsendingIT {
    private lateinit var db: Database
    private lateinit var repositories: Repositories
    private lateinit var services: Services
    private lateinit var inntektsmeldingTolker: InntektsmeldingTolker
    private val authClient = mockk<AuthClient>(relaxed = true)

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
        services = configureServices(repositories, authClient, mockk())
        inntektsmeldingTolker = InntektsmeldingTolker(services.inntektsmeldingService, repositories.mottakRepository)
    }

    @Test
    fun `les inntektsmelding på kafka og hent gjennom apiet`() {
        runTest {
            val orgnr1 = "810007982"
            inntektsmeldingTolker.lesMelding(
                buildJournalfoertInntektsmelding(orgNr = Orgnr(orgnr1)),
            )
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr1))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<InntektsmeldingFilterResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.inntektsmeldinger[0].status shouldBe InnsendingStatus.GODKJENT
            forespoerselSvar.inntektsmeldinger[0].arbeidsgiver.orgnr shouldBe orgnr1
        }
    }

    @Test
    fun `innsending av inntektsmelding på gyldig forespørsel lagres i db og lager bakgrunnsjobb`() =
        runTest {
            val requestBody = mockInntektsmeldingRequest().copy(sykmeldtFnr = DEFAULT_FNR)
            val forespoerselDokument =
                TestData
                    .forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR)
                    .copy(forespoerselId = requestBody.navReferanseId)
            repositories.forespoerselRepository.lagreForespoersel(
                forespoerselDokument.forespoerselId,
                forespoerselDokument,
            )
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status shouldBe HttpStatusCode.Created
            repositories.inntektsmeldingRepository
                .hent(requestBody.navReferanseId)
                .first()
                .navReferanseId shouldBe requestBody.navReferanseId
            repositories.bakgrunnsjobbRepository
                .findByKjoeretidBeforeAndStatusIn(
                    timeout = LocalDateTime.now(),
                    tilstander = setOf(BakgrunnsjobbStatus.OPPRETTET),
                    alle = true,
                ).first()
                .type shouldBe InnsendingProcessor.JOB_TYPE
        }

    @AfterAll
    fun shutdownStuff() {
        testApplication.stop()
        mockOAuth2Server.shutdown()
    }
}
