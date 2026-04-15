package no.nav.helsearbeidsgiver.soeknad

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
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.medOrgnr
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.genererSoeknadPdf
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.gyldigTokenxToken
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class SoeknadTokenXRoutingTest : ApiTest() {
    @AfterEach
    fun setup() {
        clearMocks(repositories.soeknadRepository)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent med TokenX person en sykepelseknad med id i PDF format`() {
        val soeknadId = UUID.randomUUID()
        val mockPdfBytes = "Mock PDF innhold".toByteArray()

        mockkStatic("no.nav.helsearbeidsgiver.utils.PdfgenUtilsKt")
        every { repositories.soeknadRepository.hentSoeknad(soeknadId) } returns
            SykepengeSoeknadDto(Random.nextLong(), soeknadMock().medId(soeknadId).medOrgnr(DEFAULT_ORG))
        every { repositories.sykmeldingRepository.hentSykmelding(any()) } returns null

        coEvery { genererSoeknadPdf(any()) } returns mockPdfBytes

        runBlocking {
            val response =
                client.get("/intern/personbruker/soknad/$soeknadId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.OK
            response.contentType() shouldBe ContentType.Application.Pdf
            response.headers[HttpHeaders.ContentDisposition] shouldBe "inline; filename=\"sykepengesoeknad-$soeknadId.pdf\""
            val pdfBytes = response.body<ByteArray>()
            pdfBytes shouldBe mockPdfBytes
        }
    }

    @Test
    fun `hent med TokenX person endepunkt skal ikke funke med en maskinporten token`() {
        val soeknadId = UUID.randomUUID()

        every { repositories.soeknadRepository.hentSoeknad(soeknadId) } returns
            SykepengeSoeknadDto(Random.nextLong(), soeknadMock().medId(soeknadId).medOrgnr(DEFAULT_ORG))
        runBlocking {
            val response =
                client.get("/intern/personbruker/soknad/$soeknadId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `hent med TokenX person som ikke har tilgang skal ikke funke`() {
        val soeknadId = UUID.randomUUID()

        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every {
            no.nav.helsearbeidsgiver.config
                .getPdpService()
                .personHarTilgang(fnr = DEFAULT_FNR, any(), any())
        } returns false

        every { repositories.soeknadRepository.hentSoeknad(soeknadId) } returns
            SykepengeSoeknadDto(Random.nextLong(), soeknadMock().medId(soeknadId).medOrgnr(DEFAULT_ORG))
        runBlocking {
            val response =
                client.get("/intern/personbruker/soknad/$soeknadId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
        unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }
}
