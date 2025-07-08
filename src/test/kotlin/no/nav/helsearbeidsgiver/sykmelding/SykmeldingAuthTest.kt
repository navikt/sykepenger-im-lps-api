package no.nav.helsearbeidsgiver.sykmelding

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
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

class SykmeldingAuthTest : ApiTest() {
    private val orgnrUtenPdpTilgang = Orgnr.genererGyldig().toString()
    private val hovedenhetOrgnrMedPdpTilgang = Orgnr.genererGyldig().toString()
    private val underenhetOrgnrMedPdpTilgang = Orgnr.genererGyldig().toString()

    @BeforeAll
    fun setup() {
        clearMocks(repositories.sykmeldingRepository)
        mockPdpTilganger()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `gir 200 OK ved henting av sykmeldinger fra deprecated endepunkt`() {
        val sykmeldingId = UUID.randomUUID()
        every { repositories.sykmeldingRepository.hentSykmeldinger(underenhetOrgnrMedPdpTilgang, null) } returns
            listOf(
                sykmeldingMock()
                    .medId(sykmeldingId.toString())
                    .medOrgnr(underenhetOrgnrMedPdpTilgang)
                    .tilSykmeldingDTO(),
            )
        runBlocking {
            val response =
                client.get("/v1/sykmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<List<Sykmelding>>()
            sykmeldingSvar.size shouldBe 1
            sykmeldingSvar[0].arbeidsgiver.orgnr.toString() shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av en spesifikk sykmelding`() {
        val sykmeldingId = UUID.randomUUID()
        every { repositories.sykmeldingRepository.hentSykmelding(sykmeldingId) } returns
            sykmeldingMock()
                .medId(sykmeldingId.toString())
                .medOrgnr(underenhetOrgnrMedPdpTilgang)
                .tilSykmeldingDTO()

        runBlocking {
            val response =
                client.get("/v1/sykmelding/$sykmeldingId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<Sykmelding>()
            sykmeldingSvar.arbeidsgiver.orgnr.toString() shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere sykmeldinger på underenhetorgnr hentet fra request`() {
        val antallForventedeSykmeldinger = 3
        every {
            repositories.sykmeldingRepository.hentSykmeldinger(
                orgnr = underenhetOrgnrMedPdpTilgang,
                filter = SykmeldingFilterRequest(orgnr = underenhetOrgnrMedPdpTilgang),
            )
        } returns
            List(
                antallForventedeSykmeldinger,
            ) {
                sykmeldingMock()
                    .medId(UUID.randomUUID().toString())
                    .medOrgnr(underenhetOrgnrMedPdpTilgang)
                    .tilSykmeldingDTO()
            }

        runBlocking {
            val response =
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykmeldingFilterRequest(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = SykmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<List<Sykmelding>>()
            sykmeldingSvar.size shouldBe antallForventedeSykmeldinger
            sykmeldingSvar.forEach {
                it.arbeidsgiver.orgnr.toString() shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere sykmeldinger på på underenhetorgnr hentet fra systembruker`() {
        val antallForventedeSykmeldinger = 3
        val requestUtenOrgnr = SykmeldingFilterRequest()
        every {
            repositories.sykmeldingRepository.hentSykmeldinger(underenhetOrgnrMedPdpTilgang, requestUtenOrgnr)
        } returns
            List(
                antallForventedeSykmeldinger,
            ) {
                sykmeldingMock()
                    .medId(UUID.randomUUID().toString())
                    .medOrgnr(underenhetOrgnrMedPdpTilgang)
                    .tilSykmeldingDTO()
            }

        runBlocking {
            val response =
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(requestUtenOrgnr.toJson(serializer = SykmeldingFilterRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<List<Sykmelding>>()
            sykmeldingSvar.size shouldBe antallForventedeSykmeldinger
            sykmeldingSvar.forEach {
                it.arbeidsgiver.orgnr.toString() shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved henting av sykmeldinger`() {
        val response1 = runBlocking { client.get("/v1/sykmeldinger") }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 = runBlocking { client.get("/v1/sykmelding/${UUID.randomUUID()}") }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val requestBody = SykmeldingFilterRequest() // TODO: Orgnr i request
        val response3 =
            runBlocking {
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = SykmeldingFilterRequest.serializer()))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når systembruker mangler i token ved henting av sykmeldinger`() {
        val response1 =
            runBlocking {
                client.get("/v1/sykmeldinger") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 =
            runBlocking {
                client.get("/v1/sykmelding/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val response3 =
            runBlocking {
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    // TODO: Underenhetorgnr i request
                    setBody(SykmeldingFilterRequest().toJson(serializer = SykmeldingFilterRequest.serializer()))
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren fra deprecated endepunkt`() {
        val sykmeldingId = UUID.randomUUID()
        every { repositories.sykmeldingRepository.hentSykmeldinger(underenhetOrgnrMedPdpTilgang, null) } returns
            listOf(
                sykmeldingMock()
                    .medId(sykmeldingId.toString())
                    .medOrgnr(underenhetOrgnrMedPdpTilgang)
                    .tilSykmeldingDTO(),
            )

        val response =
            runBlocking {
                client.get("/v1/sykmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av en spesifikk sykmelding`() {
        val sykmeldingIdIkkeTilgang = UUID.randomUUID()
        val sykmeldingIkkeTilgang =
            sykmeldingMock()
                .medId(sykmeldingIdIkkeTilgang.toString())
                .medOrgnr(orgnrUtenPdpTilgang)
                .tilSykmeldingDTO()
        every { repositories.sykmeldingRepository.hentSykmelding(sykmeldingIdIkkeTilgang) } returns sykmeldingIkkeTilgang
        val sykmeldingIdTilgang = UUID.randomUUID()
        val sykmeldingTilgang =
            sykmeldingMock()
                .medId(sykmeldingIdTilgang.toString())
                .medOrgnr(underenhetOrgnrMedPdpTilgang)
                .tilSykmeldingDTO()
        every { repositories.sykmeldingRepository.hentSykmelding(sykmeldingIdTilgang) } returns sykmeldingTilgang
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra sykmelding).
        // Det vil si at man forsøker å hente en sykmelding som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.get("/v1/sykmelding/$sykmeldingIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetorgnr (fra sykmelding).
        val response2 =
            runBlocking {
                client.get("/v1/sykmelding/$sykmeldingIdTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra sykmelding.
        val response3 =
            runBlocking {
                client.get("/v1/sykmelding/$sykmeldingIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av flere sykmeldinger`() {
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra requesten).
        // Det vil si at man forsøker å hente sykmeldinger for en organisasjon som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(SykmeldingFilterRequest(orgnr = orgnrUtenPdpTilgang).toJson(serializer = SykmeldingFilterRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetsorgnr i requesten.
        // Det vil si at man forsøker å hente sykmeldinger for et orgnummer (fra requesten), men blir nektet tilgang fra pdp pga. orgnummeret i tokenet.
        val response2 =
            runBlocking {
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykmeldingFilterRequest(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = SykmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, og har heller ikke angitt orgnr i requesten .
        // Det vil si at man forsøker å hente sykmeldinger for et orgnummer (fra tokenet) som pdp nekter systembrukeren tilgang til.
        val response3 =
            runBlocking {
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(SykmeldingFilterRequest(orgnr = null).toJson(serializer = SykmeldingFilterRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra requesten.
        val response4 =
            runBlocking {
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(SykmeldingFilterRequest(orgnr = orgnrUtenPdpTilgang).toJson(serializer = SykmeldingFilterRequest.serializer()))
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
