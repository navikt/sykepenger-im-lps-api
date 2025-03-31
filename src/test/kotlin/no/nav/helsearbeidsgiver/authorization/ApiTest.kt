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
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.config.configureKafkaConsumers
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.UUID
import org.jetbrains.exposed.sql.Database as ExposedDatabase

/*
 Denne testen kan ikke brukes med postgresql-instans, fordi det ikke er mulig å rulle tilbake endringer.
 Flyway/H2 er satt opp til å gjøre clean ved Database.init(), slik at vi ikke får problemer med data som blir igjen
 TODO: Fiks disse testene skikkelig
 */
class ApiTest {
    private val db: ExposedDatabase
    private val forespoerselRepo: ForespoerselRepository
    private val inntektsmeldingRepo: InntektsmeldingRepository

    private val port = 33445
    private val mockOAuth2Server: MockOAuth2Server
    private val testApplication: TestApplication
    private val client: HttpClient

    init {
        db = DbConfig.init()
        val repositories = configureRepositories(db)
        val services = configureServices(repositories)
        forespoerselRepo = repositories.forespoerselRepository
        inntektsmeldingRepo = repositories.inntektsmeldingRepository
        mockOAuth2Server =
            MockOAuth2Server().apply {
                start(port = port)
            }
        testApplication =
            TestApplication {
                application {
                    apiModule(services = services)
                    configureKafkaConsumers(services = services, repositories = repositories)
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
    fun `hent forespørsler fra api`() =
        runTest {
            val orgnr1 = "810007842"
            val orgnr2 = "810007843"
            val forespoerselId1 = UUID.randomUUID()
            val forespoerselId2 = UUID.randomUUID()
            val payload = forespoerselDokument(orgnr1, "123")
            forespoerselRepo.lagreForespoersel(forespoerselId1, payload)
            forespoerselRepo.lagreForespoersel(forespoerselId2, forespoerselDokument(orgnr2, "123"))

            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(gyldigSystembrukerAuthToken())
                }
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<ForespoerselResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.forespoersler[0].status shouldBe Status.AKTIV
            forespoerselSvar.forespoersler[0].orgnr shouldBe orgnr1
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
                    bearerAuth(ugyldigTokenManglerSystembruker())
                }
            response1.status.value shouldBe 401

            val response2 =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(ugyldigTokenManglerSystembruker())
                }
            response2.status.value shouldBe 401

            val requestBody = mockInntektsmeldingRequest()
            val response3 =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(ugyldigTokenManglerSystembruker())
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
            client.get("/v1/inntektsmeldinger") {
                // dette første kallet setter i gang apiModule() og database-cleanup *en ekstra gang* (!) og må kalles her fordi
                // ellers skjer dette i client.get() - steget under (og databasen nukes før vi får hentet noe...) TODO: figure out.
                bearerAuth(gyldigSystembrukerAuthToken())
            }

            inntektsmeldingRepo.opprettInntektsmelding(
                im = im,
            )
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(gyldigSystembrukerAuthToken())
                }
            response.status.value shouldBe 200
            val inntektsmeldingFilterResponse = response.body<InntektsmeldingFilterResponse>()
            inntektsmeldingFilterResponse.antall shouldBe 1
            inntektsmeldingFilterResponse.inntektsmeldinger[0].arbeidsgiver.orgnr shouldBe "810007842"
        }

    @Test
    fun `send inn inntektsmelding`() =
        runTest {
            val requestBody = mockInntektsmeldingRequest()
            val forespoersel = mockForespoersel().copy(forespoerselId = requestBody.navReferanseId, orgnr = "810007842")
            val forespoerselDokument =
                forespoerselDokument(
                    forespoersel.orgnr,
                    forespoersel.fnr,
                ).copy(forespoerselId = forespoersel.forespoerselId)
            forespoerselRepo.lagreForespoersel(forespoersel.forespoerselId, forespoerselDokument)
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(gyldigSystembrukerAuthToken())
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status.value shouldBe 201
        }

    @AfterAll
    fun shutdownStuff() {
        testApplication.stop()
        mockOAuth2Server.shutdown()
    }

    fun hentToken(claims: Map<String, Any>): String =
        mockOAuth2Server
            .issueToken(
                issuerId = "maskinporten",
                audience = "nav:helse/im.read",
                claims = claims,
            ).serialize()

    fun ugyldigTokenManglerSystembruker(): String =
        hentToken(
            claims =
                mapOf(
                    "scope" to "nav:helse/im.read",
                    "consumer" to
                        mapOf(
                            "authority" to "iso6523-actorid-upis",
                            "ID" to "0192:810007842",
                        ),
                ),
        )

    fun gyldigSystembrukerAuthToken(): String =
        hentToken(
            claims =
                mapOf(
                    "authorization_details" to
                        listOf(
                            mapOf(
                                "type" to "urn:altinn:systemuser",
                                "systemuser_id" to listOf("a_unique_identifier_for_the_systemuser"),
                                "systemuser_org" to
                                    mapOf(
                                        "authority" to "iso6523-actorid-upis",
                                        "ID" to "0192:810007842",
                                    ),
                                "system_id" to "315339138_tigersys",
                            ),
                        ),
                    "scope" to "nav:helse/im.read", // TODO sjekk om scope faktisk blir validert av tokensupport
                    "consumer" to
                        mapOf(
                            "authority" to "iso6523-actorid-upis",
                            "ID" to "0192:991825827",
                        ),
                ),
        )
}
