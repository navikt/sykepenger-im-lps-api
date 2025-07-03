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
import io.mockk.unmockkAll
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
    private val orgnrUtenPdpTilgang = Orgnr.genererGyldig().toString()
    private val hovedenhetOrgnrMedPdpTilgang = Orgnr.genererGyldig().toString()
    private val underenhetOrgnrMedPdpTilgang = Orgnr.genererGyldig().toString()

    @BeforeAll
    fun setup() {
        clearMocks(repositories.forespoerselRepository)
        mockPdpTilganger()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `gir 200 OK ved henting av forespørsler fra deprecated endepunkt`() {
        every { repositories.forespoerselRepository.hentForespoerslerForOrgnr(underenhetOrgnrMedPdpTilgang) } returns
            listOf(
                mockForespoersel().copy(orgnr = underenhetOrgnrMedPdpTilgang),
            )
        runBlocking {
            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<List<Forespoersel>>()
            forespoerselSvar.size shouldBe 1
            forespoerselSvar[0].orgnr shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av en spesifikk forespørsel`() {
        val navReferanseId = UUID.randomUUID()
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns
            mockForespoersel().copy(
                orgnr = underenhetOrgnrMedPdpTilgang,
                navReferanseId = navReferanseId,
            )

        runBlocking {
            val response =
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<Forespoersel>()
            forespoerselSvar.orgnr shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere forespørsler på underenhetorgnr hentet fra request`() {
        val antallForventedeForespoersler = 3
        every {
            repositories.forespoerselRepository.filtrerForespoersler(
                orgnr = underenhetOrgnrMedPdpTilgang,
                request = ForespoerselRequest(orgnr = underenhetOrgnrMedPdpTilgang),
            )
        } returns
            List(
                antallForventedeForespoersler,
            ) {
                mockForespoersel().copy(
                    orgnr = underenhetOrgnrMedPdpTilgang,
                    navReferanseId = UUID.randomUUID(),
                )
            }

        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest(orgnr = underenhetOrgnrMedPdpTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerslerSvar = response.body<List<Forespoersel>>()
            forespoerslerSvar.size shouldBe antallForventedeForespoersler
            forespoerslerSvar.forEach {
                it.orgnr shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere forespørsler på underenhetorgnr hentet fra systembruker`() {
        val requestUtenOrgnr = ForespoerselRequest()
        val antallForventedeForespoersler = 3
        every {
            repositories.forespoerselRepository.filtrerForespoersler(
                orgnr = underenhetOrgnrMedPdpTilgang,
                request = requestUtenOrgnr,
            )
        } returns
            List(
                antallForventedeForespoersler,
            ) {
                mockForespoersel().copy(
                    orgnr = underenhetOrgnrMedPdpTilgang,
                    navReferanseId = UUID.randomUUID(),
                )
            }

        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(requestUtenOrgnr.toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerslerSvar = response.body<List<Forespoersel>>()
            forespoerslerSvar.size shouldBe antallForventedeForespoersler
            forespoerslerSvar.forEach {
                it.orgnr shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved henting av forespørsler`() {
        val response1 = runBlocking { client.get("/v1/forespoersler") }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 = runBlocking { client.get("/v1/forespoersel/${UUID.randomUUID()}") }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val requestBody = ForespoerselRequest(orgnr = underenhetOrgnrMedPdpTilgang)
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
                    setBody(ForespoerselRequest(orgnr = underenhetOrgnrMedPdpTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren fra deprecated endepunkt`() {
        val navReferanseId = UUID.randomUUID()
        val forespoersel = mockForespoersel().copy(orgnr = orgnrUtenPdpTilgang, navReferanseId = navReferanseId)
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns forespoersel

        val response =
            runBlocking {
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av en spesifikk forespørsel`() {
        val navReferanseIdIkkeTilgang = UUID.randomUUID()
        val forespoerselIkkeTilgang =
            mockForespoersel().copy(orgnr = orgnrUtenPdpTilgang, navReferanseId = navReferanseIdIkkeTilgang)
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseIdIkkeTilgang) } returns forespoerselIkkeTilgang

        val navReferanseIdTilgang = UUID.randomUUID()
        val forespoerselTilgang =
            mockForespoersel().copy(orgnr = underenhetOrgnrMedPdpTilgang, navReferanseId = navReferanseIdTilgang)
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseIdTilgang) } returns forespoerselTilgang

        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra forespørsel).
        // Det vil si at man forsøker å hente en forespørsel som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.get("/v1/forespoersel/$navReferanseIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetorgnr (fra forespørsel).
        val response2 =
            runBlocking {
                client.get("/v1/forespoersel/$navReferanseIdTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra forespørsel.
        val response3 =
            runBlocking {
                client.get("/v1/forespoersel/$navReferanseIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av flere forespørsler`() {
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra requesten).
        // Det vil si at man forsøker å hente forespørsler for en organisasjon som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest(orgnr = orgnrUtenPdpTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetsorgnr i requesten.
        // Det vil si at man forsøker å hente forespørsler for et orgnummer (fra requesten), men blir nektet tilgang fra pdp pga. orgnummerent i tokenet.
        val response2 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest(orgnr = underenhetOrgnrMedPdpTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, og har heller ikke angitt orgnr i requesten .
        // Det vil si at man forsøker å hente forespørsler for et orgnummer (fra tokenet) som pdp nekter systembrukeren tilgang til.
        val response3 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest(orgnr = null).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra requesten.
        val response4 =
            runBlocking {
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselRequest(orgnr = orgnrUtenPdpTilgang).toJson(serializer = ForespoerselRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response4.status shouldBe HttpStatusCode.Unauthorized
    }

    private fun mockPdpTilganger() {
        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnr = orgnrUtenPdpTilgang,
                ressurs = any(),
            )
        } returns false

        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnr = match { it == hovedenhetOrgnrMedPdpTilgang || it == underenhetOrgnrMedPdpTilgang },
                ressurs = any(),
            )
        } returns true

        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnumre = match { it.contains(orgnrUtenPdpTilgang) },
                ressurs = any(),
            )
        } returns false

        every {
            getPdpService().harTilgang(
                systembruker = any(),
                orgnumre =
                    match {
                        (it.contains(hovedenhetOrgnrMedPdpTilgang) || it.contains(underenhetOrgnrMedPdpTilgang)) &&
                            !it.contains(orgnrUtenPdpTilgang)
                    },
                ressurs = any(),
            )
        } returns true
    }
}
