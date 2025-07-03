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
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.soeknad.Sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthApiTest : ApiTest() {
    @Test
    fun `gir 401 når token mangler`() =
        runTest {
            val response1 = client.get("/v1/inntektsmeldinger")
            response1.status shouldBe HttpStatusCode.Unauthorized

            val requestBody = mockInntektsmeldingRequest()
            val response2 =
                client.post("/v1/inntektsmelding") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response2.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `gir 401 når systembruker mangler i token`() =
        runTest {
            val response1 =
                client.get("/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            response1.status shouldBe HttpStatusCode.Unauthorized

            val requestBody = mockInntektsmeldingRequest()
            val response2 =
                client.post("/v1/inntektsmelding") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(serializer = InntektsmeldingRequest.serializer()))
                }
            response2.status shouldBe HttpStatusCode.Unauthorized
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
            val inntektsmeldingFilterResponse = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingFilterResponse.size shouldBe 1
            inntektsmeldingFilterResponse[0].arbeidsgiver.orgnr shouldBe DEFAULT_ORG
        }

    @Test
    fun `hent sykepengesøknader fra api`() =
        runTest {
            val orgnr = "315587336"
            every { repositories.soeknadRepository.hentSoeknader(orgnr) } returns listOf(TestData.soeknadMock())
            val response =
                client.get("/v1/sykepengesoeknader") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr))
                }
            response.status shouldBe HttpStatusCode.OK
            val soeknadResponse = response.body<List<Sykepengesoeknad>>()
            soeknadResponse.size shouldBe 1
            soeknadResponse.map { it.arbeidsgiver.orgnr } shouldContainOnly listOf(orgnr)
        }

    @Test
    fun `hent sykepengesøknad fra api`() =
        runTest {
            val orgnr = "315587336"
            val soeknad = TestData.soeknadMock()
            every { repositories.soeknadRepository.hentSoeknad(soeknad.id) } returns soeknad
            val response =
                client.get("/v1/sykepengesoeknad/${soeknad.id}") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr))
                }
            response.status shouldBe HttpStatusCode.OK
            val soeknadResponse = response.body<Sykepengesoeknad>()
            soeknadResponse.arbeidsgiver.orgnr shouldBe orgnr
            soeknadResponse.soeknadId shouldBe soeknad.id
        }
}
