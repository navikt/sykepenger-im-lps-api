package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.Producer
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test

class ApplicationTest : LpsApiIntegrasjontest() {
    @Test
    fun `leser inntektsmelding fra kafka og henter det via api`() {
        val imRecord = ProducerRecord("helsearbeidsgiver.inntektsmelding", "key", TestData.IM_MOTTATT)
        Producer.sendMelding(imRecord)
        runTest {
            val response =
                client.get("http://localhost:8080/v1/inntektsmeldinger") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"))
                }
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<InntektsmeldingFilterResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.inntektsmeldinger[0].status shouldBe InnsendingStatus.GODKJENT
            forespoerselSvar.inntektsmeldinger[0].arbeidsgiver.orgnr shouldBe "810007982"
        }
    }
}
