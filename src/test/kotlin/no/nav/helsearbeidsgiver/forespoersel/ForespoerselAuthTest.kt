package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.config.getPdpService
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

class ForespoerselAuthTest : ApiTest() {
    private val orgnrUtenTilgang = Orgnr.genererGyldig().toString()
    private val orgnrMedTilgang = Orgnr.genererGyldig().toString()

    @BeforeAll
    fun setup() {
        clearMocks(repositories.forespoerselRepository)

        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnr = orgnrUtenTilgang,
                ressurs = any(),
            )
        } returns false

        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnr = orgnrMedTilgang,
                ressurs = any(),
            )
        } returns true
    }

    @AfterAll
    fun tearDown() {
        unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }

    @Test
    fun `gir 200 OK ved henting av forespørsler fra deprecated endepunkt`() {
        every { repositories.forespoerselRepository.hentForespoerslerForOrgnr(orgnrMedTilgang) } returns
            listOf(
                mockForespoersel().copy(orgnr = orgnrMedTilgang),
            )
        runBlocking {
            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrMedTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<List<Forespoersel>>()
            forespoerselSvar.size shouldBe 1
            forespoerselSvar[0].orgnr shouldBe orgnrMedTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av en spesifikk forespørsel`() {
        val navReferanseId = UUID.randomUUID()
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns
            mockForespoersel().copy(
                orgnr = orgnrMedTilgang,
                navReferanseId = navReferanseId,
            )

        runBlocking {
            val response =
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrMedTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<Forespoersel>()
            forespoerselSvar.orgnr shouldBe orgnrMedTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av alle forespørsler på et orgnr`() {
        every {
            repositories.forespoerselRepository.filtrerForespoersler(
                ForespoerselRequest(orgnr = orgnrMedTilgang),
            )
        } returns
            List(
                3,
            ) {
                mockForespoersel().copy(
                    orgnr = orgnrMedTilgang,
                    navReferanseId = UUID.randomUUID(),
                )
            }

        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest(orgnr = orgnrMedTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrMedTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerslerSvar = response.body<List<Forespoersel>>()
            forespoerslerSvar.size shouldBe 3
            forespoerslerSvar.forEach {
                it.orgnr shouldBe orgnrMedTilgang
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved henting av forespørsler`() {
        val response1 = runBlocking { client.get("/v1/forespoersler") }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 = runBlocking { client.get("/v1/forespoersel/${UUID.randomUUID()}") }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val requestBody = ForespoerselRequest(orgnr = orgnrMedTilgang)
        val response3 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = ForespoerselRequest.serializer()))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når systembruker mangler i token ved henting av forepørsler`() {
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
                    setBody(ForespoerselRequest(orgnr = orgnrMedTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren`() {
        val navReferanseId = UUID.randomUUID()
        val forespoersel = mockForespoersel().copy(orgnr = orgnrUtenTilgang, navReferanseId = navReferanseId)
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns forespoersel

        val response1 =
            runBlocking {
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 =
            runBlocking {
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val response3 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest(orgnr = orgnrUtenTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }
}
