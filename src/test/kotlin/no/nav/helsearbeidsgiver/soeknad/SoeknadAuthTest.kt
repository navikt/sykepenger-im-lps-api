package no.nav.helsearbeidsgiver.soeknad

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
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.medOrgnr
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

// TODO: Duplisert - kan slå sammen alle *AuthTester?
class SoeknadAuthTest : ApiTest() {
    @BeforeEach
    fun setup() {
        clearMocks(repositories.soeknadRepository)
        mockPdpTilganger()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `gir 200 OK ved henting av soeknader fra deprecated endepunkt`() {
        every { repositories.soeknadRepository.hentSoeknader(underenhetOrgnrMedPdpTilgang, null) } returns
            listOf(
                soeknadMock()
                    .medOrgnr(underenhetOrgnrMedPdpTilgang),
            )
        runBlocking {
            val response =
                client.get("/v1/sykepengesoeknader") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val soeknadSvar = response.body<List<Sykepengesoeknad>>()
            soeknadSvar.size shouldBe 1
            soeknadSvar[0].arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av en spesifikk soeknad`() {
        val soeknadId = UUID.randomUUID()
        every { repositories.soeknadRepository.hentSoeknad(soeknadId) } returns
            soeknadMock()
                .medId(soeknadId)
                .medOrgnr(underenhetOrgnrMedPdpTilgang)

        runBlocking {
            val response =
                client.get("/v1/sykepengesoeknad/$soeknadId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val soeknadSvar = response.body<Sykepengesoeknad>()
            soeknadSvar.arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 404 Not Found ved henting av en spesifikk soeknad som ikke skal vises til arbeidsgiver`() {
        val soeknadId = UUID.randomUUID()
        every { repositories.soeknadRepository.hentSoeknad(soeknadId) } returns
            soeknadMock()
                .copy(sendtArbeidsgiver = null)
                .medId(soeknadId)
                .medOrgnr(underenhetOrgnrMedPdpTilgang)

        runBlocking {
            val response =
                client.get("/v1/sykepengesoeknad/$soeknadId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere soeknader på underenhetorgnr hentet fra request`() {
        val antallForventedesoeknader = 3
        val lagredeSoeknader =
            List(
                antallForventedesoeknader,
            ) {
                soeknadMock()
                    .medId(UUID.randomUUID())
                    .medOrgnr(underenhetOrgnrMedPdpTilgang)
            } +
                listOf(
                    soeknadMock()
                        .copy(sendtArbeidsgiver = null)
                        .medId(UUID.randomUUID())
                        .medOrgnr(orgnrUtenPdpTilgang),
                ) // Denne skal ikke være med i svaret))

        every {
            repositories.soeknadRepository.hentSoeknader(
                orgnr = underenhetOrgnrMedPdpTilgang,
                filter = SykepengesoeknadFilter(orgnr = underenhetOrgnrMedPdpTilgang),
            )
        } returns lagredeSoeknader

        runBlocking {
            val response =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilter(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = SykepengesoeknadFilter.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val soeknadSvar = response.body<List<Sykepengesoeknad>>()
            soeknadSvar.size shouldBe antallForventedesoeknader
            soeknadSvar.forEach {
                it.arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere soeknader på underenhetorgnr hentet fra systembruker`() {
        val antallForventedesoeknader = 3
        val requestUtenOrgnr = SykepengesoeknadFilter()
        every {
            repositories.soeknadRepository.hentSoeknader(underenhetOrgnrMedPdpTilgang, requestUtenOrgnr)
        } returns
            List(
                antallForventedesoeknader,
            ) {
                soeknadMock()
                    .medId(UUID.randomUUID())
                    .medOrgnr(underenhetOrgnrMedPdpTilgang)
            }

        runBlocking {
            val response =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(requestUtenOrgnr.toJson(serializer = SykepengesoeknadFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val soeknadSvar = response.body<List<Sykepengesoeknad>>()
            soeknadSvar.size shouldBe antallForventedesoeknader
            soeknadSvar.forEach {
                it.arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved henting av soeknader`() {
        val response1 = runBlocking { client.get("/v1/sykepengesoeknader") }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 = runBlocking { client.get("/v1/sykepengesoeknad/${UUID.randomUUID()}") }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val requestBody = SykepengesoeknadFilter(orgnr = underenhetOrgnrMedPdpTilgang)
        val response3 =
            runBlocking {
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = SykepengesoeknadFilter.serializer()))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når systembruker mangler i token ved henting av soeknader`() {
        val response1 =
            runBlocking {
                client.get("/v1/sykepengesoeknader") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 =
            runBlocking {
                client.get("/v1/sykepengesoeknad/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val response3 =
            runBlocking {
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilter(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = SykepengesoeknadFilter.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren fra deprecated endepunkt`() {
        val soeknadId = UUID.randomUUID()
        every { repositories.soeknadRepository.hentSoeknader(underenhetOrgnrMedPdpTilgang) } returns
            listOf(
                soeknadMock()
                    .medId(soeknadId)
                    .medOrgnr(underenhetOrgnrMedPdpTilgang),
            )

        val response =
            runBlocking {
                client.get("/v1/sykepengesoeknader") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av en spesifikk soeknad`() {
        val soeknadIdIkkeTilgang = UUID.randomUUID()
        val soeknadIkkeTilgang =
            soeknadMock()
                .medId(soeknadIdIkkeTilgang)
                .medOrgnr(orgnrUtenPdpTilgang)

        every { repositories.soeknadRepository.hentSoeknad(soeknadIdIkkeTilgang) } returns soeknadIkkeTilgang

        val soeknadIdTilgang = UUID.randomUUID()
        val soeknadTilgang =
            soeknadMock()
                .medId(soeknadIdTilgang)
                .medOrgnr(underenhetOrgnrMedPdpTilgang)

        every { repositories.soeknadRepository.hentSoeknad(soeknadIdTilgang) } returns soeknadTilgang
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra soeknad).
        // Det vil si at man forsøker å hente en soeknad som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.get("/v1/sykepengesoeknad/$soeknadIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetorgnr (fra soeknad).
        val response2 =
            runBlocking {
                client.get("/v1/sykepengesoeknad/$soeknadIdTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra soeknad.
        val response3 =
            runBlocking {
                client.get("/v1/sykepengesoeknad/$soeknadIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av flere soeknader`() {
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra requesten).
        // Det vil si at man forsøker å hente soeknader for en organisasjon som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(SykepengesoeknadFilter(orgnr = orgnrUtenPdpTilgang).toJson(serializer = SykepengesoeknadFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetsorgnr i requesten.
        // Det vil si at man forsøker å hente soeknader for et orgnummer (fra requesten), men blir nektet tilgang fra pdp pga. orgnummeret i tokenet.
        val response2 =
            runBlocking {
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilter(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = SykepengesoeknadFilter.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, og har heller ikke angitt orgnr i requesten .
        // Det vil si at man forsøker å hente soeknader for et orgnummer (fra tokenet) som pdp nekter systembrukeren tilgang til.
        val response3 =
            runBlocking {
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(SykepengesoeknadFilter(orgnr = null).toJson(serializer = SykepengesoeknadFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra requesten.
        val response4 =
            runBlocking {
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(SykepengesoeknadFilter(orgnr = orgnrUtenPdpTilgang).toJson(serializer = SykepengesoeknadFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response4.status shouldBe HttpStatusCode.Unauthorized
    }
}
