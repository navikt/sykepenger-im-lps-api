@file:UseSerializers(
    LocalDateSerializer::class,
    UuidSerializer::class,
)

package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID

class ForespoerselRoutingTest : ApiTest() {
    @Test
    fun `hent en spesifikk forespørsel`() {
        val navReferanseId = UUID.randomUUID()
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns
            mockForespoersel().copy(
                orgnr = DEFAULT_ORG,
                navReferanseId = navReferanseId,
            )

        runBlocking {
            val response =
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<Forespoersel>()
            forespoerselSvar.orgnr shouldBe DEFAULT_ORG
        }
    }

    @Test
    fun `hent alle forespørsler på et orgnr`() {
        val antallForventedeForespoersler = MAX_ANTALL_I_RESPONS
        every {
            repositories.forespoerselRepository.hentForespoersler(
                filter = ForespoerselFilter(orgnr = DEFAULT_ORG),
            )
        } returns
            List(
                antallForventedeForespoersler,
            ) {
                mockForespoersel().copy(
                    navReferanseId = UUID.randomUUID(),
                )
            }

        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselFilter(orgnr = DEFAULT_ORG).toJson(serializer = ForespoerselFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            response.headers["X-Warning-limit-reached"] shouldBe null // Skal ikke settes så lenge vi er innenfor MAX_ANTALL_I_RESPONS
            val forespoerslerSvar = response.body<List<Forespoersel>>()
            forespoerslerSvar.size shouldBe antallForventedeForespoersler
            forespoerslerSvar.forEach {
                it.orgnr shouldBe DEFAULT_ORG
            }
        }
    }

    @Test
    fun `hvis query gir flere enn max antall entiteter skal response begrenses og en header settes`() {
        every {
            repositories.forespoerselRepository.hentForespoersler(
                filter = ForespoerselFilter(orgnr = DEFAULT_ORG),
            )
        } returns
            List(
                MAX_ANTALL_I_RESPONS + 10,
            ) {
                mockForespoersel().copy(
                    orgnr = DEFAULT_ORG,
                    navReferanseId = UUID.randomUUID(),
                )
            }

        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselFilter(orgnr = DEFAULT_ORG).toJson(serializer = ForespoerselFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            response.headers["X-Warning-limit-reached"]?.toInt() shouldBe MAX_ANTALL_I_RESPONS
            val forespoerslerSvar = response.body<List<Forespoersel>>()
            forespoerslerSvar.size shouldBe MAX_ANTALL_I_RESPONS
        }
    }

    @Test
    fun `gir 404 Not Found dersom forespørsel ikke finnes`() {
        val navReferanseId = UUID.randomUUID()
        every { repositories.forespoerselRepository.hentForespoersel(navReferanseId) } returns null

        val response =
            runBlocking {
                client.get("/v1/forespoersel/$navReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `returnerer tom liste når det ikke er noen forespørsler på et orgnr`() {
        every {
            repositories.forespoerselRepository.hentForespoersler(
                filter = ForespoerselFilter(orgnr = DEFAULT_ORG),
            )
        } returns emptyList()

        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(ForespoerselFilter(orgnr = DEFAULT_ORG).toJson(serializer = ForespoerselFilter.serializer()))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerslerSvar = response.body<List<Forespoersel>>()
            forespoerslerSvar.size shouldBe 0
        }
    }

    @Test
    fun `gir 400 Bad Request dersom navReferanseId er ugyldig`() {
        val ugyldigNavReferanseId = "noe-helt-feil-og-ugyldig"

        val response =
            runBlocking {
                client.get("/v1/forespoersel/$ugyldigNavReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `gir 400 Bad Request dersom request mangler body`() {
        val filter = ForespoerselFilter(orgnr = DEFAULT_ORG)
        every { repositories.forespoerselRepository.hentForespoersler(filter) } returns emptyList()

        runBlocking {
            val respons =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man ber om forespørsler fra lenge før vår tidsregning`() {
        val filter = ForespoerselFilter(orgnr = DEFAULT_ORG)
        every { repositories.forespoerselRepository.hentForespoersler(filter) } returns emptyList()

        runBlocking {
            val respons =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ForespoerselFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fom = LocalDate.now().minusYears(3001),
                            tom = LocalDate.now().minusYears(3000),
                        ).toJson(
                            serializer = ForespoerselFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man ber om forespørsler for skrekkelig langt inn i fremtiden`() {
        val filter = ForespoerselFilter(orgnr = DEFAULT_ORG)
        every { repositories.forespoerselRepository.hentForespoersler(filter) } returns emptyList()

        runBlocking {
            val respons =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ForespoerselFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fom = LocalDate.now().plusYears(10000),
                            tom = LocalDate.now().plusYears(10001),
                        ).toJson(
                            serializer = ForespoerselFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["X", "*", ";Select * from forespoersel;", "heia", "98", "1234567"])
    fun `gir 400 Bad Request dersom orgnr ikke er gyldig`(orgnr: String) {
        val repo = repositories.soeknadRepository // kan ikke referere til repositories.. i verify
        runBlocking {
            val respons =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ForespoerselFilterUtenValidering(
                            orgnr = orgnr,
                        ).toJson(
                            serializer = ForespoerselFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            respons.status shouldBe HttpStatusCode.BadRequest
            verify(exactly = 0) { repo.hentSoeknader(filter = any()) }
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man forsøker å hente forespørsler fra negativt løpenummer`() {
        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ForespoerselFilterUtenValidering(
                            orgnr = DEFAULT_ORG,
                            fraLoepenr = -1,
                        ).toJson(
                            serializer = ForespoerselFilterUtenValidering.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `gir 400 Bad Request dersom man forsøker å hente forespørsler fra løpenummer høyere enn Long MAX_VALUE`() {
        runBlocking {
            val response =
                client.post("/v1/forespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ForespoerselFilterFilterSomTillaterLoepenrOverMaxLong(
                            orgnr = DEFAULT_ORG,
                            fraLoepenr = Long.MAX_VALUE.toULong() + 1UL,
                        ).toJson(
                            serializer = ForespoerselFilterFilterSomTillaterLoepenrOverMaxLong.serializer(),
                        ),
                    )
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Serializable
    data class ForespoerselFilterUtenValidering(
        val orgnr: String,
        val fnr: String? = null,
        val navReferanseId: UUID? = null,
        val status: Status? = null,
        val fom: LocalDate? = null,
        val tom: LocalDate? = null,
        val fraLoepenr: Long? = null,
    )

    @Serializable
    data class ForespoerselFilterFilterSomTillaterLoepenrOverMaxLong(
        val orgnr: String? = null,
        val fraLoepenr: ULong? = null,
    )
}
