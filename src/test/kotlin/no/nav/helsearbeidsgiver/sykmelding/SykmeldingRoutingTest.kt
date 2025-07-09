@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO.ArbeidsgiverStatusDTO
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID

class SykmeldingRoutingTest : ApiTest() {
    @AfterEach
    fun setup() {
        clearMocks(repositories.sykmeldingRepository)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent sykmeldinger fra deprecated endepunkt`() {
        val sykmeldingId = UUID.randomUUID()
        every { repositories.sykmeldingRepository.hentSykmeldinger(DEFAULT_ORG, null) } returns
            listOf(
                sykmeldingMock().medId(sykmeldingId).medOrgnr(DEFAULT_ORG).tilSykmeldingDTO(),
            )
        runBlocking {
            val response =
                client.get("/v1/sykmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<List<Sykmelding>>()
            sykmeldingSvar.size shouldBe 1
            sykmeldingSvar[0].sykmeldingId shouldBe sykmeldingId.toString()
            sykmeldingSvar[0].arbeidsgiver.orgnr.toString() shouldBe DEFAULT_ORG
        }
    }

    @Test
    fun `hent en spesifikk sykmelding`() {
        val sykmeldingId = UUID.randomUUID()
        every { repositories.sykmeldingRepository.hentSykmelding(sykmeldingId) } returns
            sykmeldingMock().medId(sykmeldingId).medOrgnr(DEFAULT_ORG).tilSykmeldingDTO()

        runBlocking {
            val response =
                client.get("/v1/sykmelding/$sykmeldingId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<Sykmelding>()
            sykmeldingSvar.sykmeldingId shouldBe sykmeldingId.toString()
            sykmeldingSvar.arbeidsgiver.orgnr.toString() shouldBe DEFAULT_ORG
        }
    }

    @Test
    fun `hent alle sykmeldinger på et orgnr`() {
        val antallForventedeSykmeldinger = 3
        every {
            repositories.sykmeldingRepository.hentSykmeldinger(
                DEFAULT_ORG,
                SykmeldingFilterRequest(orgnr = DEFAULT_ORG),
            )
        } returns
            List(
                antallForventedeSykmeldinger,
            ) {
                sykmeldingMock().medId(UUID.randomUUID()).medOrgnr(DEFAULT_ORG).tilSykmeldingDTO()
            }

        runBlocking {
            val response =
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(SykmeldingFilterRequest(orgnr = DEFAULT_ORG).toJson(serializer = SykmeldingFilterRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<List<Sykmelding>>()
            sykmeldingSvar.size shouldBe antallForventedeSykmeldinger
            sykmeldingSvar.forEach {
                it.arbeidsgiver.orgnr.toString() shouldBe DEFAULT_ORG
            }
        }
    }

    @Test
    fun `gir 404 dersom sykmelding ikke finnes`() {
        val sykmeldingId = UUID.randomUUID()
        every { repositories.sykmeldingRepository.hentSykmelding(sykmeldingId) } returns null

        val response =
            runBlocking {
                client.get("/v1/sykmelding/$sykmeldingId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `returnerer tom liste når det ikke er noen sykmeldinger på et orgnr`() {
        every { repositories.sykmeldingRepository.hentSykmeldinger(DEFAULT_ORG) } returns emptyList()

        runBlocking {
            val response =
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(SykmeldingFilterRequest(orgnr = DEFAULT_ORG).toJson(serializer = SykmeldingFilterRequest.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val sykmeldingSvar = response.body<List<Sykmelding>>()
            sykmeldingSvar.size shouldBe 0
        }
    }

    @Test
    fun `gir 400 dersom navReferanseId er ugyldig`() {
        val ugyldigNavReferanseId = "noe-helt-feil-og-ugyldig"

        val response =
            runBlocking {
                client.get("/v1/sykmelding/$ugyldigNavReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `gir 400 dersom man ber om sykmeldinger fra lenge før vår tidsregning`() {
        every { repositories.sykmeldingRepository.hentSykmeldinger(DEFAULT_ORG) } returns emptyList()

        runBlocking {
            val response =
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykmeldingFilterRequestUtenValidering(
                            fom = LocalDate.now().minusYears(3001),
                            tom = LocalDate.now().minusYears(3000),
                        ).toJson(
                            serializer = SykmeldingFilterRequestUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 dersom man ber om sykmeldinger for skrekkelig langt inn i fremtiden`() {
        every { repositories.sykmeldingRepository.hentSykmeldinger(DEFAULT_ORG) } returns emptyList()

        runBlocking {
            val response =
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykmeldingFilterRequestUtenValidering(
                            fom = LocalDate.now().minusYears(3000),
                            tom = LocalDate.now().minusYears(3001),
                        ).toJson(serializer = SykmeldingFilterRequestUtenValidering.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["X", "*", ";Select * from sykmelding;", "heia", "98", "1234567"])
    fun `Ugyldig request dersom orgnr ikke er gyldig`(orgnr: String) {
        val repo = repositories.sykmeldingRepository // kan ikke referere til repositories.. i verify
        runBlocking {
            val response =
                client.post("/v1/sykmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykmeldingFilterRequestUtenValidering(
                            orgnr = orgnr,
                        ).toJson(
                            serializer = SykmeldingFilterRequestUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
            verify(exactly = 0) { repo.hentSykmeldinger(any()) }
        }
    }

    private fun SendSykmeldingAivenKafkaMessage.medId(id: UUID) = copy(sykmelding = sykmelding.copy(id = id.toString()))

    private fun SendSykmeldingAivenKafkaMessage.medOrgnr(orgnr: String) =
        copy(event = event.copy(arbeidsgiver = ArbeidsgiverStatusDTO(orgnr, "", "")))
}

fun SendSykmeldingAivenKafkaMessage.tilSykmeldingDTO(): SykmeldingDTO =
    SykmeldingDTO(
        id = event.sykmeldingId,
        fnr = kafkaMetadata.fnr,
        orgnr = event.arbeidsgiver.orgnummer,
        sendSykmeldingAivenKafkaMessage = this,
        sykmeldtNavn = "Ola Nordmann",
        mottattAvNav = sykmelding.mottattTidspunkt.toLocalDateTime(),
    )

@Serializable
data class SykmeldingFilterRequestUtenValidering(
    val orgnr: String? = null,
    val fnr: String? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)
