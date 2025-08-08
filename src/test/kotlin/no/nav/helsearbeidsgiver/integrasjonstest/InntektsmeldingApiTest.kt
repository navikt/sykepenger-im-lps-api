package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.Test
import java.util.UUID

class InntektsmeldingApiTest : LpsApiIntegrasjontest() {
    @Test
    fun `henter inntektsmeldinger basert på status`() {
        val id1 = UUID.randomUUID()
        val inntektsmelding1 = buildInntektsmelding(inntektsmeldingId = id1, orgnr = Orgnr(DEFAULT_ORG))
        val id2 = UUID.randomUUID()
        val inntektsmelding2 = buildInntektsmelding(inntektsmeldingId = id2, orgnr = Orgnr(DEFAULT_ORG))
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding1, InnsendingStatus.GODKJENT)
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding2, InnsendingStatus.MOTTATT)
        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/status/GODKJENT",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG),
                )

            val inntektsmeldingResponse = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingResponse.size shouldBe 1
            inntektsmeldingResponse[0].status shouldBe InnsendingStatus.GODKJENT
            inntektsmeldingResponse[0].id shouldBe id1
        }
    }

    @Test
    fun `henter inntektsmeldinger basert på navReferanseId`() {
        val id1 = UUID.randomUUID()
        val im1NavReferanseId = UUID.randomUUID()
        val inntektsmelding1 =
            buildInntektsmelding(
                inntektsmeldingId = id1,
                orgnr = Orgnr(DEFAULT_ORG),
                forespoerselId = im1NavReferanseId,
            )
        val id2 = UUID.randomUUID()
        val im2NavReferanseId = UUID.randomUUID()
        val inntektsmelding2 =
            buildInntektsmelding(
                inntektsmeldingId = id2,
                orgnr = Orgnr(DEFAULT_ORG),
                forespoerselId = im2NavReferanseId,
            )
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding1)
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding2)
        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/navReferanseId/$im1NavReferanseId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG),
                )

            val inntektsmeldingResponse = response.body<List<InntektsmeldingResponse>>()
            inntektsmeldingResponse.size shouldBe 1
            inntektsmeldingResponse[0].navReferanseId shouldBe im1NavReferanseId
            inntektsmeldingResponse[0].id shouldBe id1
        }
    }

    @Test
    fun `henter inntektsmelding basert på inntektsmeldingId`() {
        val id1 = UUID.randomUUID()
        val inntektsmelding1 = buildInntektsmelding(inntektsmeldingId = id1, orgnr = Orgnr(DEFAULT_ORG))
        val id2 = UUID.randomUUID()
        val inntektsmelding2 = buildInntektsmelding(inntektsmeldingId = id2, orgnr = Orgnr(DEFAULT_ORG))
        val missingId = UUID.randomUUID()
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding1)
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding2)
        runTest {
            val ok =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$id1",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG),
                )

            val inntektsmeldingResponse = ok.body<InntektsmeldingResponse>()
            inntektsmeldingResponse.id shouldBe id1

            // Gyldig UUID, men finnes ikke i basen;
            val notFound =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$missingId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG),
                )
            notFound.status shouldBe HttpStatusCode.NotFound
            notFound.bodyAsText() shouldBe "Inntektsmelding med inntektsmeldingId: $missingId ikke funnet."

            // Ugyldig UUID:
            val ugyldig =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/1234",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG),
                )
            ugyldig.status shouldBe HttpStatusCode.BadRequest
            ugyldig.bodyAsText() shouldBe "Ugyldig identifikator"
        }
    }
}
