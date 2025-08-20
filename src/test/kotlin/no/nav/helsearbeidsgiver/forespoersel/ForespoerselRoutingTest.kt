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
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import org.junit.jupiter.api.Test
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
        val antallForventedeForespoersler = 3
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
                1100,
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
    fun `gir 404 dersom forespørsel ikke finnes`() {
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
    fun `gir 400 dersom navReferanseId er ugyldig`() {
        val ugyldigNavReferanseId = "noe-helt-feil-og-ugyldig"

        val response =
            runBlocking {
                client.get("/v1/forespoersel/$ugyldigNavReferanseId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            }
        response.status shouldBe HttpStatusCode.BadRequest
    }
}
