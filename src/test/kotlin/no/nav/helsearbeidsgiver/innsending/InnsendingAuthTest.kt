package no.nav.helsearbeidsgiver.innsending

import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.opprettImTransaction
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

// TODO: Delvis duplisert - kan muligens bakes inn i HentEntitetApiAuthTest
class InnsendingAuthTest : ApiTest() {
    @BeforeEach
    fun setup() {
        clearMocks(repositories.inntektsmeldingRepository)
        mockPdpTilganger()

        mockkStatic(Services::opprettImTransaction)
        every { services.opprettImTransaction(any(), any()) } just Runs
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `gir 200 OK ved innsending av inntektsmelding`() {
        val sykmeldtFnr = Fnr.genererGyldig().toString()
        val requestBody = mockInntektsmeldingRequest().copy(sykmeldtFnr = sykmeldtFnr)
        mockForespoerselOgInntektsmelding(
            requestBody = requestBody,
            sykmeldtFnr = sykmeldtFnr,
            orgnr = underenhetOrgnrMedPdpTilgang,
        )

        runBlocking {
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status shouldBe HttpStatusCode.Created
            verify(exactly = 1) {
                services.opprettImTransaction(
                    match { it.type.id == requestBody.navReferanseId },
                    match { it.type.id == requestBody.navReferanseId },
                )
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved innsending av inntektsmeldinger`() {
        val requestBody = mockInntektsmeldingRequest()
        val response =
            runBlocking {
                client.post("/v1/inntektsmelding") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            }
        response.status shouldBe HttpStatusCode.Unauthorized
        verify(exactly = 0) { services.opprettImTransaction(any(), any()) }
    }

    @Test
    fun `gir 401 Unauthorized når systembruker mangler i token ved innsending av inntektsmeldinger`() {
        val requestBody = mockInntektsmeldingRequest()
        val response =
            runBlocking {
                client.post("/v1/inntektsmelding") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        response.status shouldBe HttpStatusCode.Unauthorized
        verify(exactly = 0) { services.opprettImTransaction(any(), any()) }
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for innsending av inntektsmelding`() {
        val sykmeldtFnr = Fnr.genererGyldig().toString()
        val requestBodyTilgang =
            mockInntektsmeldingRequest().copy(navReferanseId = UUID.randomUUID(), sykmeldtFnr = sykmeldtFnr)
        val requestBodyIkkeTilgang =
            mockInntektsmeldingRequest().copy(navReferanseId = UUID.randomUUID(), sykmeldtFnr = sykmeldtFnr)

        mockForespoerselOgInntektsmelding(
            requestBody = requestBodyTilgang,
            sykmeldtFnr = sykmeldtFnr,
            orgnr = underenhetOrgnrMedPdpTilgang,
        )
        mockForespoerselOgInntektsmelding(
            requestBody = requestBodyIkkeTilgang,
            sykmeldtFnr = sykmeldtFnr,
            orgnr = orgnrUtenPdpTilgang,
        )

        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra forespørselen).
        // Det vil si at man forsøker å sende inn inntektsmelding for en organisasjon som systembrukeren ikke skal ha tilgang til.
        val response1 =
            runBlocking {
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                    contentType(ContentType.Application.Json)
                    setBody(requestBodyIkkeTilgang.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            }
        response1.status shouldBe HttpStatusCode.Unauthorized
        verify(exactly = 0) { services.opprettImTransaction(any(), any()) }

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetsorgnr i forespørselen.
        // Det vil si at man forsøker å sende inn en inntektsmelding for et orgnummer (fra forespørselen), men blir nektet tilgang fra pdp pga. orgnummeret i tokenet.
        val response2 =
            runBlocking {
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                    contentType(ContentType.Application.Json)
                    setBody(requestBodyTilgang.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            }
        response2.status shouldBe HttpStatusCode.Unauthorized
        verify(exactly = 0) { services.opprettImTransaction(any(), any()) }

        // Systembruker har hverken tilgang til orgnr i token eller orgnr i forespørselen.
        val response3 =
            runBlocking {
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                    contentType(ContentType.Application.Json)
                    setBody(requestBodyIkkeTilgang.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            }
        response3.status shouldBe HttpStatusCode.Unauthorized
        verify(exactly = 0) { services.opprettImTransaction(any(), any()) }
    }

    private fun mockForespoerselOgInntektsmelding(
        requestBody: InntektsmeldingRequest,
        sykmeldtFnr: String,
        orgnr: String,
    ) {
        val forespoersel =
            mockForespoersel().copy(
                navReferanseId = requestBody.navReferanseId,
                orgnr = orgnr,
                fnr = sykmeldtFnr,
            )

        every {
            repositories.forespoerselRepository.hentForespoersel(
                requestBody.navReferanseId,
            )
        } returns forespoersel

        every { repositories.forespoerselRepository.hentVedtaksperiodeId(forespoersel.navReferanseId) } returns UUID.randomUUID()
        every { repositories.inntektsmeldingRepository.hent(forespoersel.navReferanseId) } returns emptyList()
    }
}
