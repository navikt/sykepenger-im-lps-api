@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

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
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockAvvistInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID

class InntektsmeldingRoutingTest : ApiTest() {
    @BeforeEach
    fun setup() {
        clearMocks(repositories.inntektsmeldingRepository)
        every { unleashFeatureToggles.skalEksponereForespoersler() } returns true
        every { unleashFeatureToggles.skalEksponereInntektsmeldinger() } returns true
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent en spesifikk inntektsmelding`() {
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, orgnr = Orgnr(DEFAULT_ORG))
        every {
            repositories.inntektsmeldingRepository.hentMedInnsendingId(
                innsendingId = inntektsmeldingId,
            )
        } returns
            mockInntektsmeldingResponse(inntektsmelding)

        runBlocking {
            val response =
                client.get("/v1/inntektsmelding/$inntektsmeldingId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<InntektsmeldingResponse>()
            inntektsmeldingSvar.arbeidsgiver.orgnr shouldBe DEFAULT_ORG
        }
    }

    @Test
    fun `hent en avvist inntektsmelding`() {
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, orgnr = Orgnr(DEFAULT_ORG))
        every {
            repositories.inntektsmeldingRepository.hentMedInnsendingId(
                innsendingId = inntektsmeldingId,
            )
        } returns
            mockAvvistInntektsmeldingResponse(inntektsmelding)

        runBlocking {
            val response =
                client.get("/v1/inntektsmelding/$inntektsmeldingId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<InntektsmeldingResponse>()
            inntektsmeldingSvar.arbeidsgiver.orgnr shouldBe DEFAULT_ORG
            inntektsmeldingSvar.status shouldBe InnsendingStatus.FEILET
            inntektsmeldingSvar.valideringsfeil?.feilkode shouldBe Valideringsfeil.Feilkode.INNTEKT_AVVIKER_FRA_A_ORDNINGEN
        }
    }

    @Test
    fun `hent alle inntektsmeldinger på et orgnr`() {
        val antallForventedeInntektsmeldinger = 3
        every {
            repositories.inntektsmeldingRepository.hent(
                filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG),
            )
        } returns
            List(
                antallForventedeInntektsmeldinger,
            ) {
                val inntektsmelding =
                    buildInntektsmelding(
                        inntektsmeldingId = UUID.randomUUID(),
                        orgnr = Orgnr(DEFAULT_ORG),
                    )
                mockInntektsmeldingResponse(inntektsmelding)
            }

        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilter(
                            orgnr = DEFAULT_ORG,
                        ).toJson(serializer = InntektsmeldingFilter.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingSvar.size shouldBe antallForventedeInntektsmeldinger
            inntektsmeldingSvar.forEach {
                it.arbeidsgiver.orgnr shouldBe DEFAULT_ORG
            }
        }
    }

    @Test
    fun `hvis query gir flere enn max antall entiteter skal response begrenses og en header settes`() {
        every {
            repositories.inntektsmeldingRepository.hent(
                filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG),
            )
        } returns
            List(
                MAX_ANTALL_I_RESPONS + 10,
            ) {
                val inntektsmelding =
                    buildInntektsmelding(
                        inntektsmeldingId = UUID.randomUUID(),
                        orgnr = Orgnr(DEFAULT_ORG),
                    )
                mockInntektsmeldingResponse(inntektsmelding)
            }

        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilter(
                            orgnr = DEFAULT_ORG,
                        ).toJson(serializer = InntektsmeldingFilter.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            response.headers["X-Warning-limit-reached"]?.toInt() shouldBe MAX_ANTALL_I_RESPONS
            val inntektsmeldingSvar = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingSvar.size shouldBe MAX_ANTALL_I_RESPONS
        }
    }

    @Test
    fun `gir 404 Not Found dersom inntektsmelding ikke finnes`() {
        every {
            repositories.inntektsmeldingRepository.hentMedInnsendingId(any())
        } returns null

        runBlocking {
            val response =
                client.get("/v1/inntektsmelding/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `returnerer tom liste når det ikke er noen inntektsmeldinger på et orgnr`() {
        every {
            repositories.inntektsmeldingRepository.hent(
                filter = any(),
            )
        } returns emptyList()

        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilter(
                            orgnr = DEFAULT_ORG,
                        ).toJson(serializer = InntektsmeldingFilter.serializer()),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingSvar = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingSvar.size shouldBe 0
        }
    }

    @Test
    fun `gir 400 Bad Request dersom navReferanseId er ugyldig`() {
        val ugyldigNavReferanseId = "noe-helt-feil-og-ugyldig"

        val response =
            runBlocking {
                client.get("/v1/inntektsmelding/$ugyldigNavReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `gir 400 Bad Request dersom man ber om inntektsmeldinger fra lenge før vår tidsregning`() {
        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fom = LocalDate.now().minusYears(3001),
                            tom = LocalDate.now().minusYears(3000),
                        ).toJson(
                            serializer = InntektsmeldingFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom request mangler body`() {
        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man ber om inntektsmeldinger for skrekkelig langt inn i fremtiden`() {
        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fom = LocalDate.now().plusYears(10000),
                            tom = LocalDate.now().plusYears(10001),
                        ).toJson(
                            serializer = InntektsmeldingFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man forsøker å hente inntektsmeldinger uten å spesifisere orgnr i filteret`() {
        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterUtenValidering(
                            orgnr = null,
                        ).toJson(
                            serializer = InntektsmeldingFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["X", "*", ";Select * from inntektsmelding;", "heia", "98", "1234567"])
    fun `gir 400 Bad Request dersom orgnr ikke er gyldig`(orgnr: String) {
        val repo = repositories.inntektsmeldingRepository
        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterUtenValidering(
                            orgnr = orgnr,
                        ).toJson(
                            serializer = InntektsmeldingFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
            verify(exactly = 0) { repo.hent(filter = any()) }
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man forsøker å hente inntektsmeldinger fra negativt løpenummer`() {
        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fraLoepenr = -1,
                        ).toJson(
                            serializer = InntektsmeldingFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man forsøker å hente inntektsmeldinger fra løpenummer høyere enn Long MAX_VALUE`() {
        runBlocking {
            val response =
                client.post("/v1/inntektsmeldinger") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        InntektsmeldingFilterSomTillaterLoepenrOverMaxLong(
                            orgnr = DEFAULT_ORG,
                            fraLoepenr = Long.MAX_VALUE.toULong() + 1UL,
                        ).toJson(
                            serializer = InntektsmeldingFilterSomTillaterLoepenrOverMaxLong.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Serializable
    data class InntektsmeldingFilterUtenValidering(
        val orgnr: String? = null,
        val innsendingId: UUID? = null,
        val fnr: String? = null,
        val navReferanseId: UUID? = null,
        val fom: LocalDate? = null,
        val tom: LocalDate? = null,
        val status: InnsendingStatus? = null,
        val fraLoepenr: Long? = null,
    )

    @Serializable
    data class InntektsmeldingFilterSomTillaterLoepenrOverMaxLong(
        val orgnr: String? = null,
        val fraLoepenr: ULong? = null,
    )
}
