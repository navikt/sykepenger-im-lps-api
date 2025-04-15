package no.nav.helsearbeidsgiver.innsending

import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.inntektsmelding.Arbeidsgiver
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.opprettImTransaction
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class InnsendingRouteTest : ApiTest() {
    @BeforeEach
    fun setup() {
        mockkStatic(Services::opprettImTransaction)
        every { services.opprettImTransaction(any(), any()) } just Runs
    }

    @Test
    fun `innsending av inntektsmelding på gyldig forespørsel`() =
        runTest {
            val requestBody = InnsendingMockData.requestBody
            val forespoersel = InnsendingMockData.forespoersel
            every { repositories.forespoerselRepository.hentForespoersel(forespoersel.navReferanseId) } returns forespoersel
            every { repositories.inntektsmeldingRepository.hent(forespoersel.navReferanseId) } returns emptyList()
            val response = sendInnInntektsmelding(requestBody)
            response.status shouldBe HttpStatusCode.Created
            verify {
                services.opprettImTransaction(
                    match { it.type.id == requestBody.navReferanseId },
                    match { it.type.id == requestBody.navReferanseId },
                )
            }
        }

    @Test
    fun `innsending av inntektsmelding uten gyldig forespørsel gir bad request`() =
        runTest {
            val requestBody = InnsendingMockData.requestBody.copy(navReferanseId = UUID.randomUUID())
            every { repositories.forespoerselRepository.hentForespoersel(requestBody.navReferanseId) } returns null
            val response = sendInnInntektsmelding(requestBody)
            response.status shouldBe HttpStatusCode.BadRequest
            verify(exactly = 0) {
                services.opprettImTransaction(
                    match { it.type.id == requestBody.navReferanseId },
                    match { it.type.id == requestBody.navReferanseId },
                )
            }
        }

    @Test
    fun `innsending av duplikat inntektsmelding gyldig forespørsel gir conflict`() =
        runTest {
            val requestBody = InnsendingMockData.requestBody.copy(aarsakInnsending = AarsakInnsending.Endring)
            val forespoersel = InnsendingMockData.forespoersel
            every { repositories.forespoerselRepository.hentForespoersel(forespoersel.navReferanseId) } returns forespoersel
            every { repositories.inntektsmeldingRepository.hent(forespoersel.navReferanseId) } returns
                listOf(
                    InnsendingMockData.imResponse,
                )
            val response = sendInnInntektsmelding(requestBody)
            response.status shouldBe HttpStatusCode.Conflict
            verify(exactly = 0) {
                services.opprettImTransaction(
                    match { it.type.id == requestBody.navReferanseId },
                    match { it.type.id == requestBody.navReferanseId },
                )
            }
        }

    @Test
    fun `innsending av inntektsmelding med aarsak Ny der tidligere innsending finnes gir bad request`() =
        runTest {
            val requestBody = InnsendingMockData.requestBody.copy(aarsakInnsending = AarsakInnsending.Ny)
            val forespoersel = InnsendingMockData.forespoersel
            every { repositories.forespoerselRepository.hentForespoersel(forespoersel.navReferanseId) } returns forespoersel
            every { repositories.inntektsmeldingRepository.hent(forespoersel.navReferanseId) } returns
                listOf(
                    InnsendingMockData.imResponse.copy(
                        aarsakInnsending = AarsakInnsending.Ny,
                    ),
                )
            val response = sendInnInntektsmelding(requestBody)
            response.status shouldBe HttpStatusCode.BadRequest
            verify(exactly = 0) {
                services.opprettImTransaction(
                    match { it.type.id == requestBody.navReferanseId },
                    match { it.type.id == requestBody.navReferanseId },
                )
            }
        }

    @Test
    fun `innsending av inntektsmelding med aarsak ending uten tidligere innsending gir bad request`() =
        runTest {
            val requestBody = InnsendingMockData.requestBody.copy(aarsakInnsending = AarsakInnsending.Endring)
            val forespoersel = InnsendingMockData.forespoersel
            every { repositories.forespoerselRepository.hentForespoersel(forespoersel.navReferanseId) } returns forespoersel
            every { repositories.inntektsmeldingRepository.hent(forespoersel.navReferanseId) } returns emptyList()
            val response = sendInnInntektsmelding(requestBody)
            response.status shouldBe HttpStatusCode.BadRequest
            verify(exactly = 0) {
                services.opprettImTransaction(
                    match { it.type.id == requestBody.navReferanseId },
                    match { it.type.id == requestBody.navReferanseId },
                )
            }
        }

    @Test
    fun `innsending av inntektsmelding på feil orgnr gir feil`() =
        runTest {
            val requestBody = InnsendingMockData.requestBody
            val forespoersel =
                InnsendingMockData.forespoersel.copy(
                    orgnr = Orgnr.genererGyldig().verdi,
                )
            every { repositories.forespoerselRepository.hentForespoersel(forespoersel.navReferanseId) } returns forespoersel
            val response = sendInnInntektsmelding(requestBody)
            response.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `innsending av inntektsmelding på forespoersel som ikke finnes gir feil`() =
        runTest {
            val requestBody = InnsendingMockData.requestBody
            every { repositories.forespoerselRepository.hentForespoersel(requestBody.navReferanseId) } returns null
            val response = sendInnInntektsmelding(requestBody)
            response.status shouldBe HttpStatusCode.BadRequest
        }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Services::opprettImTransaction)
    }

    suspend fun sendInnInntektsmelding(request: InntektsmeldingRequest): HttpResponse =
        client.post("/v1/inntektsmelding") {
            bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
            contentType(ContentType.Application.Json)
            setBody(request.toJson(serializer = InntektsmeldingRequest.serializer()))
        }

    object InnsendingMockData {
        val requestBody = mockInntektsmeldingRequest()
        val forespoersel = mockForespoersel().copy(navReferanseId = requestBody.navReferanseId, orgnr = DEFAULT_ORG)
        val imResponse =
            mockInntektsmeldingResponse().copy(
                id = UUID.randomUUID(),
                navReferanseId = requestBody.navReferanseId,
                arbeidsgiver = Arbeidsgiver(DEFAULT_ORG, requestBody.arbeidsgiverTlf),
                inntekt = requestBody.inntekt,
                refusjon = requestBody.refusjon,
                agp = requestBody.agp,
            )
    }
}
