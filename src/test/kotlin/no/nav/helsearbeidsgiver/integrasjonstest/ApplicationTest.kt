package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.delay
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

        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmeldinger",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"),
                )

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

        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersler",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"),
                )
            val forespoerselSvar = response.body<ForespoerselResponse>()
            forespoerselSvar.antall shouldBe 1
        }
    }

    suspend fun fetchWithRetry(
        url: String,
        token: String,
        maxAttempts: Int = 5,
        delayMillis: Long = 100L,
    ): HttpResponse {
        var attempts = 0
        lateinit var response: HttpResponse

        do {
            attempts++
            response =
                client.get(url) {
                    bearerAuth(token)
                }
            if (response.status.value == 200) break
            delay(delayMillis)
        } while (attempts < maxAttempts)

        return response
    }
}
