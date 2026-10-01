package no.nav.helsearbeidsgiver.vedtak

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class RefusjonKlientTest {
    private val url = "http://hag-refusjon"
    private val refusjonUtfallId = UUID.randomUUID()

    @Test
    fun `hentRefusjonUtfallPdf skal hente PDF fra hag-refusjon`() =
        runTest {
            val pdfBytes = "PDF innhold".toByteArray()
            val mockEngine =
                MockEngine { request ->
                    request.method shouldBe HttpMethod.Get
                    request.url.toString() shouldBe "$url/refusjonsutfall/$refusjonUtfallId/pdf"
                    respond(
                        content = ByteReadChannel(pdfBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/pdf"),
                    )
                }

            refusjonKlient(mockEngine).hentRefusjonUtfallPdf(refusjonUtfallId) shouldBe pdfBytes
        }

    @Test
    fun `hentRefusjonUtfallPdf skal returnere null naar hag-refusjon ikke finner PDF`() =
        runTest {
            val mockEngine = MockEngine { respond(content = "Fant ikke refusjonsutfall", status = HttpStatusCode.NotFound) }

            refusjonKlient(mockEngine).hentRefusjonUtfallPdf(refusjonUtfallId) shouldBe null
        }

    @Test
    fun `hentRefusjonUtfallPdf skal kaste feil naar hag-refusjon svarer med serverfeil`() =
        runTest {
            val mockEngine = MockEngine { respond(content = "Feil", status = HttpStatusCode.InternalServerError) }

            assertThrows<ServerResponseException> { refusjonKlient(mockEngine).hentRefusjonUtfallPdf(refusjonUtfallId) }
        }

    private fun refusjonKlient(mockEngine: MockEngine) = RefusjonKlientImpl(url, HttpClient(mockEngine) { expectSuccess = true })
}
