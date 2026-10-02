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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.config.getPdpService
import no.nav.helsearbeidsgiver.kafka.sis.VedtaksUtfall
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfallRad
import no.nav.helsearbeidsgiver.refusjon.refusjonUtfallMock
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.gyldigTokenxToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtakRoutingTest : ApiTest() {
    @AfterEach
    fun clearRepositoryMocks() {
        clearMocks(repositories.refusjonUtfallRepository)
        unmockkObject(services.vedtakService)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent refusjonsutfall som JSON`() {
        val refusjonUtfallRad = refusjonUtfallRad()
        val refusjonUtfall = refusjonUtfallRad.refusjonUtfall
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfall.refusjonUtfallId) } returns refusjonUtfallRad

        val respons =
            runBlocking {
                client.get("/v1/refusjon/${refusjonUtfall.refusjonUtfallId}") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.OK
        runBlocking {
            respons.body<VedtakResponse>() shouldBe
                VedtakResponse(
                    loepenr = refusjonUtfallRad.loepenr,
                    vedtakId = refusjonUtfall.refusjonUtfallId,
                    orgnr = DEFAULT_ORG,
                    fom = refusjonUtfall.fom,
                    tom = refusjonUtfall.tom,
                    sykepengegrunnlag = refusjonUtfall.sykepengegrunnlag,
                    vedtaksUtfallTilArbeidsgiver = VedtaksUtfall.INNVILGELSE,
                    vedtakFattetTidspunkt = refusjonUtfall.fattetTidspunkt,
                    sykmeldtNavn = refusjonUtfall.sykmeldtNavn,
                    arbeidsgiverNavn = refusjonUtfall.arbeidsgiverNavn,
                )
        }
    }

    @Test
    fun `hent refusjonsutfall som PDF fra hag-refusjon`() {
        val refusjonUtfallRad = refusjonUtfallRad()
        val refusjonUtfallId = refusjonUtfallRad.refusjonUtfall.refusjonUtfallId
        val mockPdfBytes = "Mock PDF innhold".toByteArray()
        mockkObject(services.vedtakService)
        every { unleashFeatureToggles.skalEksponereVedtakPdf() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId) } returns refusjonUtfallRad
        coEvery { services.vedtakService.hentRefusjonUtfallPdf(refusjonUtfallId) } returns mockPdfBytes

        val respons =
            runBlocking {
                client.get("/v1/refusjon/$refusjonUtfallId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.OK
        respons.contentType() shouldBe ContentType.Application.Pdf
        respons.headers[HttpHeaders.ContentDisposition] shouldBe "inline; filename=\"refusjon-$refusjonUtfallId.pdf\""
        runBlocking {
            respons.body<ByteArray>() shouldBe mockPdfBytes
        }
    }

    @Test
    fun `hent refusjonsutfall som PDF skal svare 404 når hag-refusjon ikke finner PDF`() {
        val refusjonUtfallRad = refusjonUtfallRad()
        val refusjonUtfallId = refusjonUtfallRad.refusjonUtfall.refusjonUtfallId
        mockkObject(services.vedtakService)
        every { unleashFeatureToggles.skalEksponereVedtakPdf() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId) } returns refusjonUtfallRad
        coEvery { services.vedtakService.hentRefusjonUtfallPdf(refusjonUtfallId) } returns null

        val respons =
            runBlocking {
                client.get("/v1/refusjon/$refusjonUtfallId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `hent refusjonsutfall som PDF skal svare 500 når henting fra hag-refusjon feiler`() {
        val refusjonUtfallRad = refusjonUtfallRad()
        val refusjonUtfallId = refusjonUtfallRad.refusjonUtfall.refusjonUtfallId
        mockkObject(services.vedtakService)
        every { unleashFeatureToggles.skalEksponereVedtakPdf() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId) } returns refusjonUtfallRad
        coEvery { services.vedtakService.hentRefusjonUtfallPdf(refusjonUtfallId) } throws RuntimeException("hag-refusjon nede")

        val respons =
            runBlocking {
                client.get("/v1/refusjon/$refusjonUtfallId/pdf") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.InternalServerError
    }

    @Test
    fun `hent refusjonsutfall skal svare 403 når feature toggle er av`() {
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns false

        val respons =
            runBlocking {
                client.get("/v1/refusjon/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `hent refusjonsutfall skal svare 400 for ugyldig refusjonUtfallId`() {
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true

        val respons =
            runBlocking {
                client.get("/v1/refusjon/ugyldig") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `hent refusjonsutfall skal svare 404 når refusjonsutfallet ikke finnes`() {
        val refusjonUtfallId = UUID.randomUUID()
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId) } returns null

        val respons =
            runBlocking {
                client.get("/v1/refusjon/$refusjonUtfallId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `hent refusjonsutfall skal svare 403 uten tilgang til inntektsmeldingressursen`() {
        val refusjonUtfallRad = refusjonUtfallRad()
        val refusjonUtfallId = refusjonUtfallRad.refusjonUtfall.refusjonUtfallId
        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId) } returns refusjonUtfallRad
        every {
            getPdpService()
                .harTilgang(systembruker = any(), orgnr = DEFAULT_ORG, ressurs = any())
        } returns false

        val respons =
            runBlocking {
                client.get("/v1/refusjon/$refusjonUtfallId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Forbidden
        unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }

    @Test
    fun `systembrukerendepunktet skal avvise TokenX token`() {
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true

        val respons =
            runBlocking {
                client.get("/v1/refusjon/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.gyldigTokenxToken(DEFAULT_FNR))
                }
            }

        respons.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `hent flere refusjonsutfall som JSON`() {
        val filter = VedtakFilter(orgnr = DEFAULT_ORG, fnr = DEFAULT_FNR)
        val refusjonUtfallRader = listOf(refusjonUtfallRad(loepenr = 1), refusjonUtfallRad(loepenr = 2))
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true
        every { repositories.refusjonUtfallRepository.hentRefusjonUtfall(filter) } returns refusjonUtfallRader

        val respons =
            runBlocking {
                client.post("/v1/refusjon") {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(serializer = VedtakFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.OK
        runBlocking {
            respons.body<List<VedtakResponse>>().map { it.vedtakId } shouldBe refusjonUtfallRader.map { it.refusjonUtfall.refusjonUtfallId }
        }
    }

    @Test
    fun `hent flere refusjonsutfall skal svare 403 når feature toggle er av`() {
        val filter = VedtakFilter(orgnr = DEFAULT_ORG)
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns false

        val respons =
            runBlocking {
                client.post("/v1/refusjon") {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(serializer = VedtakFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `hent flere refusjonsutfall skal svare 403 uten tilgang til inntektsmeldingressursen`() {
        val filter = VedtakFilter(orgnr = DEFAULT_ORG)
        mockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true
        every {
            getPdpService()
                .harTilgang(systembruker = any(), orgnr = DEFAULT_ORG, ressurs = any())
        } returns false

        val respons =
            runBlocking {
                client.post("/v1/refusjon") {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(serializer = VedtakFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.Forbidden
        unmockkStatic("no.nav.helsearbeidsgiver.config.ApplicationConfigKt")
    }

    @Test
    fun `hent flere refusjonsutfall skal svare 400 for ugyldig filter`() {
        every { unleashFeatureToggles.skalEksponereVedtakJson() } returns true

        val respons =
            runBlocking {
                client.post("/v1/refusjon") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"orgnr":"ugyldig"}""")
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        respons.status shouldBe HttpStatusCode.BadRequest
    }

    private fun refusjonUtfallRad(loepenr: Long = 1) =
        RefusjonUtfallRad(
            loepenr = loepenr,
            refusjonUtfall = refusjonUtfallMock().copy(fnr = Fnr(DEFAULT_FNR), orgnr = Orgnr(DEFAULT_ORG)),
        )
}
