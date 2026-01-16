package no.nav.helsearbeidsgiver.utils

// Using Ktor MockEngine for proper HttpClient testing
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingModelMock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class PdfgenUtilsTest {
    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `genererSykmeldingPdf skal sende POST til PDFGEN_URL med sykmelding JSON og returnere bytes`() =
        runTest {
            val testSykmelding = sykmeldingModelMock()
            val forventetBytes = "PDF innhold".toByteArray()

            val mockEngine =
                MockEngine { request ->
                    PDFGEN_URL shouldBe getPropertyOrNull("PDFGEN_SYKMELDING_URL")
                    request.url.toString() shouldBe PDFGEN_URL

                    respond(
                        content = ByteReadChannel(forventetBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/pdf"),
                    )
                }

            mockHttpClient(mockEngine)
            val result = genererSykmeldingPdf(testSykmelding)

            result shouldBe forventetBytes
        }

    @Test
    fun `genererSykmeldingPdf skal throw RuntimeException om status kode ikke er 200`() =
        runTest {
            val testSykmelding = sykmeldingModelMock()

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = ByteReadChannel("Error"),
                        status = HttpStatusCode.InternalServerError,
                    )
                }

            mockHttpClient(mockEngine)

            shouldThrow<RuntimeException> {
                genererSykmeldingPdf(testSykmelding)
            }
        }
}

private fun mockHttpClient(mockEngine: MockEngine) {
    val mockHttpClient =
        HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
    mockkStatic("no.nav.helsearbeidsgiver.utils.UtilsKt")
    every { createHttpClient() } returns mockHttpClient
}