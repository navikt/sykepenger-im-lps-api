package no.nav.helsearbeidsgiver.inntektsmelding

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
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

// TODO: Duplisert - kan slå sammen alle *AuthTester?
class InntektsmeldingAuthTest : ApiTest() {
    @BeforeEach
    fun setup() {
        clearMocks(repositories.inntektsmeldingRepository)
        mockPdpTilganger()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `gir 200 OK ved henting av inntektsmeldinger fra deprecated endepunkt`() {
        val inntektsmelding =
            buildInntektsmelding(orgNr = Orgnr(underenhetOrgnrMedPdpTilgang))
        every { repositories.inntektsmeldingRepository.hent(underenhetOrgnrMedPdpTilgang) } returns
            listOf(
                mockInntektsmeldingResponse(inntektsmelding),
            )
        runBlocking {
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingSvar.size shouldBe 1
            inntektsmeldingSvar[0].arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av en spesifikk inntektsmelding med inntektsmeldingId`() {
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, orgNr = Orgnr(underenhetOrgnrMedPdpTilgang))
        every {
            repositories.inntektsmeldingRepository.hentMedInnsendingId(
                innsendingId = inntektsmeldingId,
            )
        } returns
            mockInntektsmeldingResponse(inntektsmelding)

        runBlocking {
            val response =
                client.get("/v1/inntektsmelding/$inntektsmeldingId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<InntektsmeldingResponse>()
            inntektsmeldingSvar.arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere inntektsmeldinger på underenhetorgnr hentet fra request`() {
        val antallForventedeInntektsmeldinger = 3
        every {
            repositories.inntektsmeldingRepository.hent(
                orgNr = underenhetOrgnrMedPdpTilgang,
                request = InntektsmeldingFilterRequest(orgnr = underenhetOrgnrMedPdpTilgang),
            )
        } returns
            List(
                antallForventedeInntektsmeldinger,
            ) {
                val inntektsmelding =
                    buildInntektsmelding(
                        inntektsmeldingId = UUID.randomUUID(),
                        orgNr = Orgnr(underenhetOrgnrMedPdpTilgang),
                    )
                mockInntektsmeldingResponse(inntektsmelding)
            }

        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterRequest(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = InntektsmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingSvar.size shouldBe antallForventedeInntektsmeldinger
            inntektsmeldingSvar.forEach {
                it.arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere inntektsmeldinger på underenhetorgnr hentet fra systembruker`() {
        val antallForventedeInntektsmeldinger = 3
        val requestUtenOrgnr = InntektsmeldingFilterRequest()
        every {
            repositories.inntektsmeldingRepository.hent(
                orgNr = underenhetOrgnrMedPdpTilgang,
                request = InntektsmeldingFilterRequest(),
            )
        } returns
            List(
                antallForventedeInntektsmeldinger,
            ) {
                val inntektsmelding =
                    buildInntektsmelding(
                        inntektsmeldingId = UUID.randomUUID(),
                        orgNr = Orgnr(underenhetOrgnrMedPdpTilgang),
                    )
                mockInntektsmeldingResponse(inntektsmelding)
            }

        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        requestUtenOrgnr.toJson(serializer = InntektsmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingSvar.size shouldBe antallForventedeInntektsmeldinger
            inntektsmeldingSvar.forEach {
                it.arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved henting av inntektsmeldinger`() {
        val response1 = runBlocking { client.get("/v1/inntektsmeldinger") }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 = runBlocking { client.get("/v1/inntektsmelding/${UUID.randomUUID()}") }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val requestBody = InntektsmeldingFilterRequest(orgnr = underenhetOrgnrMedPdpTilgang)
        val response3 =
            runBlocking {
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingFilterRequest.serializer()))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når systembruker mangler i token ved henting av inntektsmeldinger`() {
        val response1 =
            runBlocking {
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        val response2 =
            runBlocking {
                client.get("/v1/inntektsmelding/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        val response3 =
            runBlocking {
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterRequest(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = InntektsmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren fra deprecated endepunkt`() {
        val inntektsmelding =
            buildInntektsmelding(orgNr = Orgnr(underenhetOrgnrMedPdpTilgang))
        every { repositories.inntektsmeldingRepository.hent(underenhetOrgnrMedPdpTilgang) } returns
            listOf(
                mockInntektsmeldingResponse(inntektsmelding),
            )
        runBlocking {
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av en spesifikk inntektsmelding`() {
        val inntektsmeldingIdIkkeTilgang = UUID.randomUUID()
        val inntektsmeldingIkkeTilgang =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingIdIkkeTilgang, orgNr = Orgnr(orgnrUtenPdpTilgang))
        every {
            repositories.inntektsmeldingRepository.hentMedInnsendingId(
                innsendingId = inntektsmeldingIdIkkeTilgang,
            )
        } returns mockInntektsmeldingResponse(inntektsmeldingIkkeTilgang)

        val inntektsmeldingIdTilgang = UUID.randomUUID()
        val inntektsmeldingTilgang =
            buildInntektsmelding(
                inntektsmeldingId = inntektsmeldingIdTilgang,
                orgNr = Orgnr(underenhetOrgnrMedPdpTilgang),
            )
        every {
            repositories.inntektsmeldingRepository.hentMedInnsendingId(
                innsendingId = inntektsmeldingIdTilgang,
            )
        } returns mockInntektsmeldingResponse(inntektsmeldingTilgang)

        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra inntektsmelding).
        // Det vil si at man forsøker å hente en inntektsmelding som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.get("/v1/inntektsmelding/$inntektsmeldingIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetorgnr (fra inntektsmelding).
        val response2 =
            runBlocking {
                client.get("/v1/inntektsmelding/$inntektsmeldingIdTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra inntektsmelding.
        val response3 =
            runBlocking {
                client.get("/v1/inntektsmelding/$inntektsmeldingIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av flere inntektsmeldinger`() {
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra requesten).
        // Det vil si at man forsøker å hente inntektsmeldinger for en organisasjon som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterRequest(
                            orgnr = orgnrUtenPdpTilgang,
                        ).toJson(serializer = InntektsmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetsorgnr i requesten.
        // Det vil si at man forsøker å hente inntektsmeldinger for et orgnummer (fra requesten), men blir nektet tilgang fra pdp pga. orgnummeret i tokenet.
        val response2 =
            runBlocking {
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterRequest(
                            orgnr = underenhetOrgnrMedPdpTilgang,
                        ).toJson(serializer = InntektsmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, og har heller ikke angitt orgnr i requesten .
        // Det vil si at man forsøker å hente inntektsmeldinger for et orgnummer (fra tokenet) som pdp nekter systembrukeren tilgang til.
        val response3 =
            runBlocking {
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(InntektsmeldingFilterRequest(orgnr = null).toJson(serializer = InntektsmeldingFilterRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra requesten.
        val response4 =
            runBlocking {
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterRequest(
                            orgnr = orgnrUtenPdpTilgang,
                        ).toJson(serializer = InntektsmeldingFilterRequest.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        response4.status shouldBe HttpStatusCode.Unauthorized
    }
}
