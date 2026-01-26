package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helsearbeidsgiver.apiModule
import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.JournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilter
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.NotisType
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.buildJournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.utils.getTestLeaderConfig
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@WithPostgresContainer
class InnsendingIT {
    private lateinit var db: Database
    private lateinit var repositories: Repositories
    private lateinit var services: Services
    private lateinit var inntektsmeldingTolker: InntektsmeldingTolker
    private val authClient = mockk<AuthClient>(relaxed = true)
    private val unleashFeatureToggles = mockk<UnleashFeatureToggles>(relaxed = true)
    private val port = 33445
    private val mockOAuth2Server =
        MockOAuth2Server().apply {
            start(port = port)
        }
    private val testApplication =
        TestApplication {
            application {
                apiModule(services = services, authClient = authClient, unleashFeatureToggles)
            }
        }
    private val client =
        testApplication.createClient {
            install(ContentNegotiation) {
                json()
            }
        }

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        repositories = configureRepositories(db)
        services =
            configureServices(
                repositories = repositories,
                unleashFeatureToggles = unleashFeatureToggles,
                database = db,
                pdlService = mockk(),
                leaderConfig = getTestLeaderConfig(isLeader = false),
            )
        inntektsmeldingTolker =
            InntektsmeldingTolker(
                mottakRepository = repositories.mottakRepository,
                services = services,
            )
        every { unleashFeatureToggles.skalEksponereInntektsmeldinger() } returns true
    }

    @Test
    fun `les inntektsmelding på kafka og hent gjennom apiet`() {
        runTest {
            val orgnr1 = "810007982"
            val melding = buildJournalfoertInntektsmelding(orgnr = Orgnr(orgnr1))
            inntektsmeldingTolker.lesMelding(
                melding,
            )
            val innsendtJson = melding.fromJson(JournalfoertInntektsmelding.serializer())
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(InntektsmeldingFilter(orgnr = orgnr1).toJson(serializer = InntektsmeldingFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr1))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingSvar.size shouldBe 1
            inntektsmeldingSvar[0].status shouldBe InnsendingStatus.GODKJENT
            inntektsmeldingSvar[0].arbeidsgiver.orgnr shouldBe orgnr1
            inntektsmeldingSvar[0].arbeidsgiver.kontaktinformasjon shouldBe innsendtJson.inntektsmelding.avsender.navn
        }
    }

    @Test
    fun `innsending av inntektsmelding på gyldig forespørsel lagres i db og lager bakgrunnsjobb`() =
        runTest {
            val requestBody = mockInntektsmeldingRequest().copy(sykmeldtFnr = DEFAULT_FNR)
            val forespoerselDokument =
                TestData
                    .forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR)
                    .copy(forespoerselId = requestBody.navReferanseId)
            services.forespoerselService.lagreNyForespoersel(
                forespoerselDokument,
            )
            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status shouldBe HttpStatusCode.Created
            val innsendtIM =
                repositories.inntektsmeldingRepository
                    .hent(requestBody.navReferanseId)
                    .first()
            innsendtIM
                .navReferanseId shouldBe requestBody.navReferanseId
            innsendtIM.arbeidsgiver.kontaktinformasjon shouldBe requestBody.kontaktinformasjon
            repositories.bakgrunnsjobbRepository
                .findByKjoeretidBeforeAndStatusIn(
                    timeout = LocalDateTime.now(),
                    tilstander = setOf(BakgrunnsjobbStatus.OPPRETTET),
                    alle = true,
                ).first()
                .type shouldBe InnsendingProcessor.JOB_TYPE
        }

// TODO kommentert ut midlertidig. Denne feiler med Mottatt ugyldig innsending: Ugyldig arbeidsgiverperiode. Oppgi arbeidsgiverperiode bare ved nytt sykefravær der første fraværsdag er mer enn 16 dager etter forrige sykefraværsdag.
    @Test
    fun `innsending av inntektsmelding merger forespurtdata hvis et finnes en besvart fra før`() =
        runTest {
            val navReferanseId = UUID.randomUUID()
            val requestBody = mockInntektsmeldingRequest().copy(navReferanseId = navReferanseId, sykmeldtFnr = DEFAULT_FNR)
            val vedtaksperiodeId = UUID.randomUUID()
            val forespoersel1 =
                TestData
                    .forespoerselDokument(
                        orgnr = DEFAULT_ORG,
                        fnr = DEFAULT_FNR,
                        vedtaksperiodeId = vedtaksperiodeId,
                        agpPaakrevd = true,
                        inntektPaakrevd = false,
                    )

            services.forespoerselService.lagreNyForespoersel(
                forespoersel1,
                status = Status.BESVART,
            )
            val forespoersel2 =
                TestData
                    .forespoerselDokument(
                        forespoerselId = navReferanseId,
                        orgnr = DEFAULT_ORG,
                        fnr = DEFAULT_FNR,
                        vedtaksperiodeId = vedtaksperiodeId,
                        agpPaakrevd = false,
                        inntektPaakrevd = true,
                    )

            services.forespoerselService.lagreOppdatertForespoersel(
                PriMessage(notis = NotisType.FORESPOERSEL_OPPDATERT, forespoersel2),
            )

            val response =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response.status shouldBe HttpStatusCode.Created
        }

    @AfterAll
    fun shutdownStuff() =
        runBlocking {
            testApplication.stop()
            mockOAuth2Server.shutdown()
        }
}
