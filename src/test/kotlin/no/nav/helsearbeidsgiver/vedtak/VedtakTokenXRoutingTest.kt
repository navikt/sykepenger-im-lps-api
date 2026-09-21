package no.nav.helsearbeidsgiver.vedtak

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import no.nav.helsearbeidsgiver.utils.genererVedtakPdf
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.gyldigTokenxToken
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtakTokenXRoutingTest : ApiTest() {
    @AfterEach
    fun setup() {
        clearMocks(repositories.vedtakRepository)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent med TokenX person et vedtak med id i PDF format`() {
        val vedtakId = UUID.randomUUID()
        val mockPdfBytes = "Mock PDF innhold".toByteArray()
        val vedtak = vedtakMock()

        mockkStatic("no.nav.helsearbeidsgiver.utils.PdfgenUtilsKt")
        every { unleashFeatureToggles.skalEksponereVedtakPdf() } returns true
        every { repositories.vedtakRepository.hentVedtak(vedtakId) } returns
            VedtakRad(
                loepenr = 1,
                vedtakId = vedtakId,
                fnr = DEFAULT_FNR,
                orgnr = DEFAULT_ORG,
                vedtak = vedtak,
            )

        coEvery { genererVedtakPdf(any()) } returns mockPdfBytes

        runBlocking {
            val response =
                client.get("/intern/personbruker/vedtak/$vedtakId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.OK
            response.contentType() shouldBe ContentType.Application.Pdf
            response.headers[HttpHeaders.ContentDisposition] shouldBe "inline; filename=\"vedtak-$vedtakId.pdf\""
            val pdfBytes = response.body<ByteArray>()
            pdfBytes shouldBe mockPdfBytes
        }
    }

    @Test
    fun `hent med TokenX person endepunkt skal ikke funke med en maskinporten token`() {
        val vedtakId = UUID.randomUUID()

        every { unleashFeatureToggles.skalEksponereVedtakPdf() } returns true
        runBlocking {
            val response =
                client.get("/intern/personbruker/vedtak/$vedtakId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `hent med TokenX person som ikke har tilgang skal ikke funke`() {
        val vedtakId = UUID.randomUUID()
        val vedtak = vedtakMock()

        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every { unleashFeatureToggles.skalEksponereVedtakPdf() } returns true
        every {
            no.nav.helsearbeidsgiver.config
                .getPdpService()
                .personHarTilgang(fnr = DEFAULT_FNR, any(), any())
        } returns false

        every { repositories.vedtakRepository.hentVedtak(vedtakId) } returns
            VedtakRad(
                loepenr = 1,
                vedtakId = vedtakId,
                fnr = DEFAULT_FNR,
                orgnr = DEFAULT_ORG,
                vedtak = vedtak,
            )
        runBlocking {
            val response =
                client.get("/intern/personbruker/vedtak/$vedtakId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.Forbidden
        }
        unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }

    @Test
    fun `hent vedtak PDF skal svare 403 naar feature toggle er av`() {
        val vedtakId = UUID.randomUUID()

        every { unleashFeatureToggles.skalEksponereVedtakPdf() } returns false

        runBlocking {
            val response =
                client.get("/intern/personbruker/vedtak/$vedtakId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }
}
