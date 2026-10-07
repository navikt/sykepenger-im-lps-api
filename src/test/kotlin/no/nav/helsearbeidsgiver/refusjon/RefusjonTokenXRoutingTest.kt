package no.nav.helsearbeidsgiver.refusjon

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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.gyldigTokenxToken
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class RefusjonTokenXRoutingTest : ApiTest() {
    @AfterEach
    fun setup() {
        clearMocks(repositories.refusjonUtfallRepository)
        unmockkObject(services.refusjonUtfallService)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent med TokenX person et refusjonsutfall med id i PDF format`() {
        val refusjonUtfallId = UUID.randomUUID()
        val mockPdfBytes = "Mock PDF innhold".toByteArray()
        val refusjonUtfall =
            refusjonUtfallResponseMock()
                .copy(
                    refusjonUtfallId = refusjonUtfallId,
                    fnr = Fnr(DEFAULT_FNR),
                    orgnr = Orgnr(DEFAULT_ORG),
                )

        mockkObject(services.refusjonUtfallService)
        every { unleashFeatureToggles.skalEksponereRefusjonUtfallPdf() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId) } returns refusjonUtfall
        coEvery { services.refusjonUtfallService.hentRefusjonUtfallPdf(refusjonUtfallId) } returns mockPdfBytes

        runBlocking {
            val response =
                client.get("/intern/personbruker/refusjon/$refusjonUtfallId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.OK
            response.contentType() shouldBe ContentType.Application.Pdf
            response.headers[HttpHeaders.ContentDisposition] shouldBe "inline; filename=\"refusjon-$refusjonUtfallId.pdf\""
            val pdfBytes = response.body<ByteArray>()
            pdfBytes shouldBe mockPdfBytes
        }
    }

    @Test
    fun `hent med TokenX person endepunkt skal ikke funke med en maskinporten token`() {
        every { unleashFeatureToggles.skalEksponereRefusjonUtfallPdf() } returns true
        runBlocking {
            val response =
                client.get("/intern/personbruker/refusjon/${UUID.randomUUID()}/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `hent med TokenX person som ikke har tilgang skal ikke funke`() {
        val refusjonUtfallId = UUID.randomUUID()
        val refusjonUtfall =
            refusjonUtfallResponseMock()
                .copy(
                    refusjonUtfallId = refusjonUtfallId,
                    fnr = Fnr(DEFAULT_FNR),
                    orgnr = Orgnr(DEFAULT_ORG),
                )

        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every { unleashFeatureToggles.skalEksponereRefusjonUtfallPdf() } returns true
        every {
            no.nav.helsearbeidsgiver.config
                .getPdpService()
                .personHarTilgang(fnr = DEFAULT_FNR, any(), any())
        } returns false

        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId) } returns refusjonUtfall
        runBlocking {
            val response =
                client.get("/intern/personbruker/refusjon/$refusjonUtfallId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.Forbidden
        }
        unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }

    @Test
    fun `hent refusjonsutfall PDF skal svare 403 naar feature toggle er av`() {
        every { unleashFeatureToggles.skalEksponereRefusjonUtfallPdf() } returns false

        runBlocking {
            val response =
                client.get("/intern/personbruker/refusjon/${UUID.randomUUID()}/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }
}
