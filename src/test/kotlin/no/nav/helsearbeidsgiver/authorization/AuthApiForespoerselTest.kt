package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.config.getPdpService
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRequest
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthApiForespoerselTest : ApiTest() {
    @Test
    fun `hent forespørsler fra deprecated api-endepunkt`() =
        runTest {
            every { repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG) } returns
                listOf(
                    mockForespoersel(),
                )
            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<List<Forespoersel>>()
            forespoerselSvar.size shouldBe 1
            forespoerselSvar[0].status shouldBe Status.AKTIV
            forespoerselSvar[0].orgnr shouldBe DEFAULT_ORG
        }

    @Test
    fun `hent én spesifikk forespørsel fra apiet`() =
        runTest {
            val navReferanseId = UUID.randomUUID()
            every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns
                mockForespoersel().copy(
                    navReferanseId = navReferanseId,
                )

            val response =
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<Forespoersel>()
            forespoerselSvar.status shouldBe Status.AKTIV
            forespoerselSvar.orgnr shouldBe DEFAULT_ORG
        }

    @Test
    fun `gir 404 dersom forespørsel ikke finnes`() {
        val navReferanseId = UUID.randomUUID()
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns null

        val response =
            runBlocking {
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `gir 404 dersom navReferanseId er ugyldig`() {
        val ugyldigNavReferanseId = "noe-helt-feil-og-ugyldig"

        val response =
            runBlocking {
                client.get("/v1/forespoersel/$ugyldigNavReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `gir 401 når token mangler ved henting av forespørsler`() =
        runTest {
            val response1 = client.get("/v1/forespoersler")
            response1.status shouldBe HttpStatusCode.Unauthorized

            val response2 = client.get("/v1/forespoersel/${UUID.randomUUID()}")
            response2.status shouldBe HttpStatusCode.Unauthorized

            val requestBody = ForespoerselRequest()
            val response3 =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = ForespoerselRequest.serializer()))
                }
            response3.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `gir 401 når systembruker mangler i token ved henting av forepørsler`() {
        val response1 =
            runBlocking {
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 =
            runBlocking {
                client.get("/v1/forespoersel/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val response3 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest().toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 når pdp nekter tilgang for systembrukeren`() {
        val navReferanseId = UUID.randomUUID()
        val forespoersel = mockForespoersel().copy(navReferanseId = navReferanseId)
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns forespoersel

        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnr = forespoersel.orgnr,
                ressurs = any(),
            )
        } returns false

        val response1 =
            runBlocking {
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 =
            runBlocking {
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val response3 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest().toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }
}
