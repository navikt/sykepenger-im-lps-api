package no.nav.helsearbeidsgiver.vedtak

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import no.nav.helsearbeidsgiver.utils.genererVedtakPdf
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.gyldigTokenxToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtakRoutingTest : ApiTest() {
    @AfterEach
    fun clearRepositoryMocks() {
        clearMocks(repositories.vedtakRepository)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent vedtak som JSON`() {
        val vedtakId = UUID.randomUUID()
        val loepenr = 42L
        val vedtak = vedtakMock()
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true
        every { repositories.vedtakRepository.hentVedtak(vedtakId) } returns
            VedtakRad(
                loepenr = loepenr,
                vedtakId = vedtakId,
                fnr = DEFAULT_FNR,
                orgnr = DEFAULT_ORG,
                vedtak = vedtak,
            )

        val respons =
            runBlocking {
                client.get("/v1/vedtak/$vedtakId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.OK
        runBlocking {
            respons.body<VedtakResponse>() shouldBe
                VedtakResponse(
                    loepenr = loepenr,
                    vedtakId = vedtakId,
                    orgnr = DEFAULT_ORG,
                    fom = vedtak.fom,
                    tom = vedtak.tom,
                    sykepengegrunnlag = vedtak.sykepengegrunnlag,
                    vedtaksUtfallTilArbeidsgiver = vedtak.vedtaksUtfallTilArbeidsgiver,
                    vedtakFattetTidspunkt = vedtak.vedtakFattetTidspunkt,
                )
        }
    }

    @Test
    fun `hent vedtak som PDF`() {
        val vedtakId = UUID.randomUUID()
        val mockPdfBytes = "Mock PDF innhold".toByteArray()
        mockkStatic("no.nav.helsearbeidsgiver.utils.PdfgenUtilsKt")
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true
        every { repositories.vedtakRepository.hentVedtak(vedtakId) } returns vedtakRad(vedtakId)
        coEvery { genererVedtakPdf(any()) } returns mockPdfBytes

        val respons =
            runBlocking {
                client.get("/v1/vedtak/$vedtakId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.OK
        respons.contentType() shouldBe ContentType.Application.Pdf
        respons.headers[HttpHeaders.ContentDisposition] shouldBe "inline; filename=\"vedtak-$vedtakId.pdf\""
        runBlocking {
            respons.body<ByteArray>() shouldBe mockPdfBytes
        }
    }

    @Test
    fun `hent vedtak skal svare 403 naar feature toggle er av`() {
        every { unleashFeatureToggles.skalEksponereVedtak() } returns false

        val respons =
            runBlocking {
                client.get("/v1/vedtak/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `hent vedtak skal svare 400 for ugyldig vedtakId`() {
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true

        val respons =
            runBlocking {
                client.get("/v1/vedtak/ugyldig") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `hent vedtak skal svare 404 naar vedtaket ikke finnes`() {
        val vedtakId = UUID.randomUUID()
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true
        every { repositories.vedtakRepository.hentVedtak(vedtakId) } returns null

        val respons =
            runBlocking {
                client.get("/v1/vedtak/$vedtakId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `hent vedtak skal svare 401 uten tilgang til inntektsmeldingressursen`() {
        val vedtakId = UUID.randomUUID()
        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true
        every { repositories.vedtakRepository.hentVedtak(vedtakId) } returns vedtakRad(vedtakId)
        every {
            no.nav.helsearbeidsgiver.config
                .getPdpService()
                .harTilgang(systembruker = any(), orgnr = DEFAULT_ORG, ressurs = any())
        } returns false

        val respons =
            runBlocking {
                client.get("/v1/vedtak/$vedtakId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Unauthorized
        io.mockk.unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }

    @Test
    fun `systembrukerendepunktet skal avvise TokenX token`() {
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true

        val respons =
            runBlocking {
                client.get("/v1/vedtak/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            }

        respons.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `hent flere vedtak som JSON`() {
        val filter = VedtakFilter(orgnr = DEFAULT_ORG, fnr = DEFAULT_FNR)
        val vedtak = listOf(vedtakRad(UUID.randomUUID()), vedtakRad(UUID.randomUUID()))
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true
        every { repositories.vedtakRepository.hentVedtak(filter) } returns vedtak

        val respons =
            runBlocking {
                client.post("/v1/vedtak") {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(serializer = VedtakFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.OK
        runBlocking {
            respons.body<List<VedtakResponse>>().map { it.vedtakId } shouldBe vedtak.map { it.vedtakId }
        }
    }

    @Test
    fun `hent flere vedtak skal svare 403 naar feature toggle er av`() {
        val filter = VedtakFilter(orgnr = DEFAULT_ORG)
        every { unleashFeatureToggles.skalEksponereVedtak() } returns false

        val respons =
            runBlocking {
                client.post("/v1/vedtak") {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(serializer = VedtakFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `hent flere vedtak skal svare 401 uten tilgang til inntektsmeldingressursen`() {
        val filter = VedtakFilter(orgnr = DEFAULT_ORG)
        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true
        every {
            no.nav.helsearbeidsgiver.config
                .getPdpService()
                .harTilgang(systembruker = any(), orgnr = DEFAULT_ORG, ressurs = any())
        } returns false

        val respons =
            runBlocking {
                client.post("/v1/vedtak") {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(serializer = VedtakFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Unauthorized
        io.mockk.unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }

    @Test
    fun `hent flere vedtak skal svare 400 for ugyldig filter`() {
        every { unleashFeatureToggles.skalEksponereVedtak() } returns true

        val respons =
            runBlocking {
                client.post("/v1/vedtak") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"orgnr":"ugyldig"}""")
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.BadRequest
    }

    private fun vedtakRad(vedtakId: UUID) =
        VedtakRad(
            loepenr = 1,
            vedtakId = vedtakId,
            fnr = DEFAULT_FNR,
            orgnr = DEFAULT_ORG,
            vedtak = vedtakMock(),
        )
}
