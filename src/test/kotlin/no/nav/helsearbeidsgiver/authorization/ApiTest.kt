package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.Tolkers
import no.nav.helsearbeidsgiver.config.configureKafkaConsumers
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.config.configureTolkers
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

@WithPostgresContainer
class ApiTest {
    private lateinit var db: Database
    private val repositories: Repositories = mockk<Repositories>(relaxed = true)
    private val services: Services = configureServices(repositories)
    private val tolkers: Tolkers = configureTolkers(services, repositories)

    private val port = 33445
    private val mockOAuth2Server: MockOAuth2Server =
        MockOAuth2Server().apply {
            start(port = port)
        }
    private val testApplication: TestApplication =
        TestApplication {
            application {
                apiModule(services = services)
                configureKafkaConsumers(tolkers)
            }
        }
    private val client: HttpClient =
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
    }

    @Test
    fun `hent forespørsler fra api`() =
        runTest {
            every { repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG) } returns listOf(mockForespoersel())
            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<ForespoerselResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.forespoersler[0].status shouldBe Status.AKTIV
            forespoerselSvar.forespoersler[0].orgnr shouldBe DEFAULT_ORG
        }

    @Test
    fun `gir 401 når token mangler`() =
        runTest {
            val response1 = client.get("/v1/forespoersler")
            response1.status.value shouldBe 401

            val response2 = client.get("/v1/inntektsmeldinger")
            response2.status.value shouldBe 401

            val requestBody = mockInntektsmeldingRequest()
            val response3 =
                client.post("/v1/inntektsmelding") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response3.status.value shouldBe 401
        }

    @Test
    fun `gir 401 når systembruker mangler i token`() =
        runTest {
            val response1 =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker(DEFAULT_ORG))
                }
            response1.status.value shouldBe 401

            val response2 =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker(DEFAULT_ORG))
                }
            response2.status.value shouldBe 401

            val requestBody = mockInntektsmeldingRequest()
            val response3 =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker(DEFAULT_ORG))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response3.status.value shouldBe 401
        }

    @Test
    fun `hent inntektsmeldinger`() =
        runTest {
            val forespoerselId = UUID.fromString("13129b6c-e9f5-4b1c-a855-abca47ac3d7f")
            val im = buildInntektsmelding(forespoerselId = forespoerselId)
            every { repositories.inntektsmeldingRepository.hent(DEFAULT_ORG) } returns
                listOf(
                    mockInntektsmeldingResponse(im),
                )
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status.value shouldBe 200
            val inntektsmeldingFilterResponse = response.body<InntektsmeldingFilterResponse>()
            inntektsmeldingFilterResponse.antall shouldBe 1
            inntektsmeldingFilterResponse.inntektsmeldinger[0].arbeidsgiver.orgnr shouldBe DEFAULT_ORG
        }

    @Test
    fun `innsending av inntektsmelding på gyldig forespørsel`() =
        runTest {
            val requestBody = mockInntektsmeldingRequest()
            val forespoersel = mockForespoersel().copy(navReferanseId = requestBody.navReferanseId, orgnr = DEFAULT_ORG)
            every { repositories.forespoerselRepository.hentForespoersel(forespoersel.navReferanseId) } returns forespoersel
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status.value shouldBe 201
        }

    @Test
    fun `innsending av inntektsmelding på feil orgnr gir feil`() =
        runTest {
            val requestBody = mockInntektsmeldingRequest()
            val forespoersel =
                mockForespoersel().copy(
                    navReferanseId = requestBody.navReferanseId,
                    orgnr = Orgnr.genererGyldig().verdi,
                )
            every { repositories.forespoerselRepository.hentForespoersel(forespoersel.navReferanseId) } returns forespoersel
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status.value shouldBe 400
        }

    @Test
    fun `innsending av inntektsmelding på forespoersel som ikke finnes gir feil`() =
        runTest {
            val requestBody = mockInntektsmeldingRequest()
            every { repositories.forespoerselRepository.hentForespoersel(requestBody.navReferanseId) } returns null
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status.value shouldBe 400
        }

    @AfterAll
    fun shutdownStuff() {
        testApplication.stop()
        mockOAuth2Server.shutdown()
    }
}
