package no.nav.helsearbeidsgiver.inntekt

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.getPdpService
import no.nav.helsearbeidsgiver.pdp.IPdpService
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.plugins.Feil
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntektRoutingTest {
    private val inntektService = mockk<InntektService>()
    private val pdpService = mockk<IPdpService>()

    private val services =
        Services(
            forespoerselService = mockk(relaxed = true),
            inntektsmeldingService = mockk(relaxed = true),
            inntektService = inntektService,
            innsendingService = mockk(relaxed = true),
            dokumentkoblingService = mockk(relaxed = true),
            sykmeldingService = mockk(relaxed = true),
            pdlService = mockk(relaxed = true),
            soeknadService = mockk(relaxed = true),
            helseSjekkService = mockk(relaxed = true),
            avvistInntektsmeldingService = mockk(relaxed = true),
            vedtakService = mockk(relaxed = true),
        )

    private val mockOAuth2Server =
        MockOAuth2Server().apply {
            start(port = 33445)
        }

    private val testApplication =
        TestApplication {
            application {
                apiModule(
                    services = services,
                    authClient = mockk(relaxed = true),
                )
            }
        }

    private val client =
        testApplication.createClient {
            install(ContentNegotiation) {
                json()
            }
        }

    @BeforeAll
    fun setupStaticMocks() {
        mockkStatic(::getPdpService)
    }

    @BeforeEach
    fun setup() {
        clearMocks(inntektService, pdpService)
        every { getPdpService() } returns pdpService
    }

    @AfterAll
    fun teardown() =
        runBlocking {
            testApplication.stop()
            mockOAuth2Server.shutdown()
            unmockkStatic(::getPdpService)
        }

    @Test
    fun `hent inntekt returnerer 200 med inntekt og gjennomsnitt`() {
        val navReferanseId = UUID.randomUUID()
        val inntektsdatoString = "2024-04-15"
        val forespoersel = mockForespoersel().copy(navReferanseId = navReferanseId, orgnr = DEFAULT_ORG)
        val responseBody =
            InntektMedGjennomsnittResponse(
                inntektPerMaaned =
                    mapOf(
                        YearMonth.of(2024, 1) to 100.0,
                        YearMonth.of(2024, 2) to null,
                        YearMonth.of(2024, 3) to 300.0,
                    ),
            )

        every { services.forespoerselService.hentForespoersel(navReferanseId) } returns forespoersel
        every { pdpService.harTilgang(any(), DEFAULT_ORG, any()) } returns true
        coEvery {
            services.inntektService.hentInntekter(
                forespoersel = forespoersel,
                inntektsdato = LocalDate.of(2024, 4, 15),
            )
        } returns responseBody

        val response =
            runBlocking {
                client.get("/v1/inntekt?navReferanseId=$navReferanseId&inntektsdato=$inntektsdatoString") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        response.status shouldBe HttpStatusCode.OK
        runBlocking { response.body<InntektMedGjennomsnittResponse>() } shouldBe responseBody
        coVerify(exactly = 1) { services.inntektService.hentInntekter(forespoersel, LocalDate.of(2024, 4, 15)) }
    }

    @Test
    fun `hent inntekt returnerer 400 ved ugyldig navReferanseId`() {
        val response =
            runBlocking {
                client.get("/v1/inntekt?navReferanseId=ugyldig&inntektsdato=2024-04-15") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        response.status shouldBe HttpStatusCode.BadRequest
        runBlocking { response.body<ErrorResponse>().feilkode } shouldBe Feil.UGYLDIG_NAV_REFERANSE_ID.name
    }

    @Test
    fun `hent inntekt returnerer 400 ved ugyldig inntektsdato`() {
        val navReferanseId = UUID.randomUUID()

        val response =
            runBlocking {
                client.get("/v1/inntekt?navReferanseId=$navReferanseId&inntektsdato=20240415") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        response.status shouldBe HttpStatusCode.BadRequest
        runBlocking { response.body<ErrorResponse>().feilkode } shouldBe Feil.UGYLDIG_DATO.name
    }

    @Test
    fun `hent inntekt returnerer 404 når forespoersel ikke finnes`() {
        val navReferanseId = UUID.randomUUID()
        every { services.forespoerselService.hentForespoersel(navReferanseId) } returns null

        val response =
            runBlocking {
                client.get("/v1/inntekt?navReferanseId=$navReferanseId&inntektsdato=2024-04-15") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `hent inntekt returnerer 401 når systembruker ikke har tilgang`() {
        val navReferanseId = UUID.randomUUID()
        val forespoersel = mockForespoersel().copy(navReferanseId = navReferanseId, orgnr = DEFAULT_ORG)
        every { services.forespoerselService.hentForespoersel(navReferanseId) } returns forespoersel
        every { pdpService.harTilgang(any(), DEFAULT_ORG, any()) } returns false

        val response =
            runBlocking {
                client.get("/v1/inntekt?navReferanseId=$navReferanseId&inntektsdato=2024-04-15") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }

        response.status shouldBe HttpStatusCode.Unauthorized
        runBlocking { response.body<ErrorResponse>().feilkode } shouldBe Feil.IKKE_TILGANG_TIL_RESSURS.name
        coVerify(exactly = 0) { inntektService.hentInntekter(any(), any()) }
    }
}
