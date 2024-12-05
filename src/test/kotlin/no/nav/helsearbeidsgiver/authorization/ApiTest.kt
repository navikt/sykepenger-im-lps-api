package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.Database as ExposedDatabase

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiTest {
    private val db: ExposedDatabase
    private val forespoerselRepo: ForespoerselRepository

    private val port = 33445
    private val mockOAuth2Server: MockOAuth2Server
    private val testApplication: TestApplication
    private val client: HttpClient

    init {
        mockOAuth2Server =
            MockOAuth2Server().apply {
                start(port = port)
            }
        db = Database.init()
        forespoerselRepo = ForespoerselRepository(db)
        testApplication = TestApplication {}
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
                client.get("/forespoersler") {
                    bearerAuth(gyldigAuthToken())
                }
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<ForespoerselResponse>()
            forespoerselSvar.antallForespoersler shouldBe 1
            forespoerselSvar.forespoerseler[0].status shouldBe Status.AKTIV
            forespoerselSvar.forespoerseler[0].orgnr shouldBe orgnr1
            forespoerselSvar.forespoerseler[0].dokument shouldBe payload
        }

    @Test
    fun `gir 401 når token mangler`() =
        runTest {
            val response1 = client.get("/forespoersler")
            response1.status.value shouldBe 401
            val response2 = client.get("/inntektsmeldinger")
            response2.status.value shouldBe 401
        }

    @Test
    fun `gir 401 når supplier mangler i token`() =
        runTest {
            val response1 =
                client.get("/forespoersler") {
                    bearerAuth(ugyldigTokenManglerSupplier())
                }
            response1.status.value shouldBe 401
            val response2 =
                client.get("/inntektsmeldinger") {
                    bearerAuth(ugyldigTokenManglerSupplier())
                }
            response2.status.value shouldBe 401
        }

    @Test
    fun `hent inntektsmeldinger`() =
        runTest {
            val forespoerselId = "13129b6c-e9f5-4b1c-a855-abca47ac3d7f"
            val im = buildInntektsmelding(forespoerselId = forespoerselId)
            InntektsmeldingRepository(db).opprett(im, "810007842", "12345678912", LocalDateTime.now(), forespoerselId)

            val response =
                client.get("/inntektsmeldinger") {
                    bearerAuth(gyldigAuthToken())
                }
            response.status.value shouldBe 200
            val inntektsmeldingResponse = response.body<InntektsmeldingResponse>()
            inntektsmeldingResponse.antallInntektsmeldinger shouldBe 1
            inntektsmeldingResponse.inntektsmeldinger[0].orgnr shouldBe "810007842"
        }

    @AfterAll
    fun shutdownStuff() {
        mockOAuth2Server.shutdown()
    }

    fun hentToken(claims: Map<String, Any>): String =
        mockOAuth2Server
            .issueToken(
                issuerId = "maskinporten",
                audience = "nav:helse/im.read",
                claims = claims,
            ).serialize()

    fun gyldigAuthToken(): String =
        hentToken(
            claims =
                mapOf(
                    "scope" to "maskinporten",
                    "supplier" to
                        mapOf(
                            "authority" to "iso6523-actorid-upis",
                            "ID" to "0192:991825827",
                        ),
                    "consumer" to
                        mapOf(
                            "authority" to "iso6523-actorid-upis",
                            "ID" to "0192:810007842",
                        ),
                ),
        )

    fun ugyldigTokenManglerSupplier(): String =
        hentToken(
            claims =
                mapOf(
                    "scope" to "maskinporten",
                    "consumer" to
                        mapOf(
                            "authority" to "iso6523-actorid-upis",
                            "ID" to "0192:810007842",
                        ),
                ),
        )
}
