package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.Test
import java.util.UUID

class InntektsmeldingApiTest : LpsApiIntegrasjontest() {
    @Test
    fun `henter inntektsmeldinger basert på status`() {
        val im1Id = UUID.randomUUID()
        val inntektsmelding1 = buildInntektsmelding(inntektsmeldingId = im1Id, orgNr = Orgnr("810007982"))
        val im2Id = UUID.randomUUID()
        val inntektsmelding2 = buildInntektsmelding(inntektsmeldingId = im2Id, orgNr = Orgnr("810007982"))
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding1, InnsendingStatus.GODKJENT)
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding2, InnsendingStatus.MOTTATT)
        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/status/GODKJENT",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"),
                )

            val inntektsmeldingResponse = response.body<InntektsmeldingFilterResponse>()
            inntektsmeldingResponse.antall shouldBe 1
            inntektsmeldingResponse.inntektsmeldinger[0].status shouldBe InnsendingStatus.GODKJENT
            inntektsmeldingResponse.inntektsmeldinger[0].id shouldBe im1Id
        }
    }

    @Test
    fun `henter inntektsmeldinger basert på navReferanseId`() {
        val im1Id = UUID.randomUUID()
        val im1NavReferanseId = UUID.randomUUID()
        val inntektsmelding1 =
            buildInntektsmelding(
                inntektsmeldingId = im1Id,
                orgNr = Orgnr("810007982"),
                forespoerselId = im1NavReferanseId,
            )
        val im2Id = UUID.randomUUID()
        val im2NavReferanseId = UUID.randomUUID()
        val inntektsmelding2 =
            buildInntektsmelding(
                inntektsmeldingId = im2Id,
                orgNr = Orgnr("810007982"),
                forespoerselId = im2NavReferanseId,
            )
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding1)
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding2)
        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/navReferanseId/$im1NavReferanseId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"),
                )

            val inntektsmeldingResponse = response.body<InntektsmeldingFilterResponse>()
            inntektsmeldingResponse.antall shouldBe 1
            inntektsmeldingResponse.inntektsmeldinger[0].navReferanseId shouldBe im1NavReferanseId
            inntektsmeldingResponse.inntektsmeldinger[0].id shouldBe im1Id
        }
    }

    @Test
    fun `henter inntektsmeldinger basert på inntektsmeldingId`() {
        val im1Id = UUID.randomUUID()
        val inntektsmelding1 = buildInntektsmelding(inntektsmeldingId = im1Id, orgNr = Orgnr("810007982"))
        val im2Id = UUID.randomUUID()
        val inntektsmelding2 = buildInntektsmelding(inntektsmeldingId = im2Id, orgNr = Orgnr("810007982"))
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding1)
        repositories.inntektsmeldingRepository.opprettInntektsmelding(inntektsmelding2)
        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$im1Id",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"),
                )

            val inntektsmeldingResponse = response.body<InntektsmeldingFilterResponse>()
            inntektsmeldingResponse.antall shouldBe 1
            inntektsmeldingResponse.inntektsmeldinger[0].id shouldBe im1Id
        }
    }
}
