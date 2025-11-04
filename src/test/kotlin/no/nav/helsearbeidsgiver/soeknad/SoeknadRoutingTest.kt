@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.soeknad

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
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.medOrgnr
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class SoeknadRoutingTest : ApiTest() {
    @BeforeEach
    fun beforeEach() {
        every { unleashFeatureToggles.skalEksponereSykepengesoeknader() } returns true
        every { unleashFeatureToggles.skalEksponereSykmeldinger(Orgnr(DEFAULT_ORG)) } returns true
    }

    @AfterEach
    fun setup() {
        clearMocks(repositories.soeknadRepository)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `hent en spesifikk søknad`() {
        val soeknad = SykepengeSoeknadDto(Random.nextLong(), TestData.soeknadMock().medOrgnr(DEFAULT_ORG))
        every { repositories.soeknadRepository.hentSoeknad(soeknad.sykepengeSoeknadKafkaMelding.id) } returns soeknad

        runBlocking {
            val respons =
                client.get("/v1/sykepengesoeknad/${soeknad.sykepengeSoeknadKafkaMelding.id}") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.OK
            val soeknadRespons = respons.body<Sykepengesoeknad>()
            soeknadRespons.arbeidsgiver.orgnr shouldBe DEFAULT_ORG
            soeknadRespons.soeknadId shouldBe soeknad.sykepengeSoeknadKafkaMelding.id
        }
    }

    @Test
    fun `hent alle søknader på et orgnr`() {
        val antallForventedeSoeknader = 3
        every {
            repositories.soeknadRepository.hentSoeknader(filter = SykepengesoeknadFilter(orgnr = DEFAULT_ORG))
        } returns
            List(
                antallForventedeSoeknader,
            ) {
                SykepengeSoeknadDto(
                    Random.nextLong(1, 100),
                    TestData.soeknadMock().medOrgnr(DEFAULT_ORG).medId(UUID.randomUUID()),
                )
            }

        runBlocking {
            val respons =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(SykepengesoeknadFilter(orgnr = DEFAULT_ORG).toJson(serializer = SykepengesoeknadFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.OK
            val soeknadRespons = respons.body<List<Sykepengesoeknad>>()
            soeknadRespons.size shouldBe antallForventedeSoeknader
            soeknadRespons.forEach {
                it.arbeidsgiver.orgnr shouldBe DEFAULT_ORG
            }
        }
    }

    @Test
    fun `gir 404 Not Found dersom søknad ikke finnes`() {
        val soeknadId = UUID.randomUUID()
        every { repositories.soeknadRepository.hentSoeknad(soeknadId) } returns null

        val respons =
            runBlocking {
                client.get("/v1/sykepengesoeknad/$soeknadId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        respons.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `gir 404 Not Found dersom navReferanseId er ugyldig`() {
        val ugyldigNavReferanseId = "noe-helt-feil-og-ugyldig"

        val respons =
            runBlocking {
                client.get("/v1/sykepengesoeknad/$ugyldigNavReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        respons.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `gir 400 Bad Request dersom man ber om søknader fra lenge før vår tidsregning`() {
        val filter = SykepengesoeknadFilter(orgnr = DEFAULT_ORG)
        every { repositories.soeknadRepository.hentSoeknader(filter) } returns emptyList()

        runBlocking {
            val respons =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fom = LocalDate.now().minusYears(3001),
                            tom = LocalDate.now().minusYears(3000),
                        ).toJson(
                            serializer = SykepengesoeknadFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom request mangler body`() {
        val filter = SykepengesoeknadFilter(orgnr = DEFAULT_ORG)
        every { repositories.soeknadRepository.hentSoeknader(filter) } returns emptyList()

        runBlocking {
            val respons =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man ber om søknader for skrekkelig langt inn i fremtiden`() {
        val filter = SykepengesoeknadFilter(orgnr = DEFAULT_ORG)
        every { repositories.soeknadRepository.hentSoeknader(filter) } returns emptyList()

        runBlocking {
            val respons =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fom = LocalDate.now().plusYears(10000),
                            tom = LocalDate.now().plusYears(10001),
                        ).toJson(
                            serializer = SykepengesoeknadFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["X", "*", ";Select * from soeknad;", "heia", "98", "1234567"])
    fun `gir 400 Bad Request ugyldig request dersom orgnr ikke er gyldig`(orgnr: String) {
        val repo = repositories.soeknadRepository // kan ikke referere til repositories.. i verify
        runBlocking {
            val respons =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilterUtenValidering(
                            orgnr = orgnr,
                        ).toJson(
                            serializer = SykepengesoeknadFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
            verify(exactly = 0) { repo.hentSoeknader(filter = any()) }
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man forsøker å hente sykepengesøknader fra negativt løpenummer`() {
        runBlocking {
            val response =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fraLoepenr = -1,
                        ).toJson(
                            serializer = SykepengesoeknadFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man forsøker å hente sykepengesøknader fra løpenummer høyere enn Long MAX_VALUE`() {
        runBlocking {
            val response =
                client.post("/v1/sykepengesoeknader") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SykepengesoeknadFilterSomTillaterLoepenrOverMaxLong(
                            orgnr = DEFAULT_ORG,
                            fraLoepenr = Long.MAX_VALUE.toULong() + 1UL,
                        ).toJson(
                            serializer = SykepengesoeknadFilterSomTillaterLoepenrOverMaxLong.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Serializable
    data class SykepengesoeknadFilterUtenValidering(
        val orgnr: String? = null,
        val fnr: String? = null,
        val fom: LocalDate? = null,
        val tom: LocalDate? = null,
        val fraLoepenr: Long? = null,
    )

    @Serializable
    data class SykepengesoeknadFilterSomTillaterLoepenrOverMaxLong(
        val orgnr: String? = null,
        val fraLoepenr: ULong? = null,
    )
}
