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
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockSkjemaInntektsmelding
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
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
        db = Database.init()
        forespoerselRepo = ForespoerselRepository(db)
        inntektsmeldingRepo = InntektsmeldingRepository(db)
        mockOAuth2Server =
            MockOAuth2Server().apply {
                start(port = port)
            }
        testApplication =
            TestApplication {
                application {
                    apiModule()
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
            val payload = forespoerselDokument(orgnr1, "123")
            forespoerselRepo.lagreForespoersel("123", payload)
            forespoerselRepo.lagreForespoersel("1234", forespoerselDokument(orgnr2, "123"))

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

            val requestBody = mockSkjemaInntektsmelding()
            val response3 =
                client.post("/v1/inntektsmelding") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = SkjemaInntektsmelding.serializer()))
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

            val requestBody = mockSkjemaInntektsmelding()
            val response3 =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(ugyldigTokenManglerSystembruker())
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = SkjemaInntektsmelding.serializer()))
                }
            response3.status.value shouldBe 401
        }

    @Test
    fun `hent inntektsmeldinger`() =
        runTest {
            val forespoerselId = "13129b6c-e9f5-4b1c-a855-abca47ac3d7f"
            val im = buildInntektsmelding(forespoerselId = forespoerselId)
            client.get("/v1/inntektsmeldinger") {
                // dette første kallet setter i gang apiModule() og database-cleanup *en ekstra gang* (!) og må kalles her fordi
                // ellers skjer dette i client.get() - steget under (og databasen nukes før vi får hentet noe...) TODO: figure out.
                bearerAuth(gyldigSystembrukerAuthToken())
            }

            inntektsmeldingRepo.opprettInntektsmeldingFraSimba(
                im = im,
                org = "810007842",
                sykmeldtFnr = "12345678912",
                innsendtDato = LocalDateTime.now(),
                forespoerselID = forespoerselId,
            )
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(gyldigSystembrukerAuthToken())
                }
            response.status.value shouldBe 200
            val inntektsmeldingResponse = response.body<InntektsmeldingResponse>()
            inntektsmeldingResponse.antall shouldBe 1
            inntektsmeldingResponse.inntektsmeldinger[0].orgnr shouldBe "810007842"
        }

    @Test
    fun `send inn inntektsmelding`() =
        runTest {
            val requestBody = mockSkjemaInntektsmelding()
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(gyldigSystembrukerAuthToken())
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = SkjemaInntektsmelding.serializer()))
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
