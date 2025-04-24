package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.Producer
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.buildInntektsmeldingJsonNy
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test

class ApplicationTest : LpsApiIntegrasjontest() {
    @Test
    fun `leser inntektsmelding fra kafka og henter det via api`() {
        val imRecord =
            ProducerRecord("helsearbeidsgiver.inntektsmelding", "key", buildInntektsmeldingJsonNy(orgNr = Orgnr("810007982")))
        Producer.sendMelding(imRecord)
        Thread.sleep(1000)
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
            forespoerselSvar.inntektsmeldinger[0].navReferanseId shouldNotBe "24428a05-6826-4a01-a6be-30fb15816a6eno thanks"
        }
    }

    @Test
    fun `leser forespoersel fra kafka og henter det via api`() {
        val priRecord = ProducerRecord("helsearbeidsgiver.pri", "key", TestData.FORESPOERSEL_MOTTATT)
        Producer.sendMelding(priRecord)
        Thread.sleep(1000)
        runTest {
            val response =
                client.get("http://localhost:8080/v1/forespoersler") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"))
                }
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<ForespoerselResponse>()
            forespoerselSvar.antall shouldBe 1
        }
    }
}
