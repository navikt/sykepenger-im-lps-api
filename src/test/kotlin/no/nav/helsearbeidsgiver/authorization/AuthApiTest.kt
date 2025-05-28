package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.collections.shouldContainOnly
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
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.soknad.Sykepengesoknad
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthApiTest : ApiTest() {
    @Test
    fun `hent forespørsler fra api`() =
        runTest {
            every { repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG) } returns
                listOf(
                    mockForespoersel(),
                )
            val response =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val forespoerselSvar = response.body<ForespoerselResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.forespoersler[0].status shouldBe Status.AKTIV
            forespoerselSvar.forespoersler[0].orgnr shouldBe DEFAULT_ORG
        }

    @Test
    fun `gir 401 når token mangler`() =
        runTest {
            val response1 = client.get("/v1/forespoersler")
            response1.status shouldBe HttpStatusCode.Unauthorized

            val response2 = client.get("/v1/inntektsmeldinger")
            response2.status shouldBe HttpStatusCode.Unauthorized

            val requestBody = mockInntektsmeldingRequest()
            val response3 =
                client.post("/v1/inntektsmelding") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response3.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `gir 401 når systembruker mangler i token`() =
        runTest {
            val response1 =
                client.get("/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            response1.status shouldBe HttpStatusCode.Unauthorized

            val response2 =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            response2.status shouldBe HttpStatusCode.Unauthorized

            val requestBody = mockInntektsmeldingRequest()
            val response3 =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response3.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `hent inntektsmeldinger`() =
        runTest {
            val forespoerselId = UUID.fromString("13129b6c-e9f5-4b1c-a855-abca47ac3d7f")
            val im = buildInntektsmelding(forespoerselId = forespoerselId)
            every { repositories.inntektsmeldingRepository.hent(DEFAULT_ORG) } returns
                listOf(
                    mockInntektsmeldingResponse(im),
                )
            val response =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG))
                }
            response.status shouldBe HttpStatusCode.OK
            val inntektsmeldingFilterResponse = response.body<InntektsmeldingFilterResponse>()
            inntektsmeldingFilterResponse.antall shouldBe 1
            inntektsmeldingFilterResponse.inntektsmeldinger[0].arbeidsgiver.orgnr shouldBe DEFAULT_ORG
        }

    @Test
    fun `hent soknader fra api`() =
        runTest {
            val orgnr = "315587336"
            every { repositories.soknadRepository.hentSoknader(orgnr) } returns listOf(TestData.soknadMock())
            val response =
                client.get("/v1/sykepengesoknader") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr))
                }
            response.status shouldBe HttpStatusCode.OK
            val soknadResponse = response.body<List<Sykepengesoknad>>()
            soknadResponse.size shouldBe 1
            soknadResponse.map { it.arbeidsgiver.orgnr } shouldContainOnly listOf(orgnr)
        }

    @Test
    fun `hent soknad fra api`() =
        runTest {
            val orgnr = "315587336"
            val soknad = TestData.soknadMock()
            every { repositories.soknadRepository.hentSoknad(soknad.id) } returns soknad
            val response =
                client.get("/v1/sykepengesoknad/${soknad.id}") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr))
                }
            response.status shouldBe HttpStatusCode.OK
            val soknadResponse = response.body<Sykepengesoknad>()
            soknadResponse.arbeidsgiver.orgnr shouldBe orgnr
            soknadResponse.id shouldBe soknad.id
        }
}
