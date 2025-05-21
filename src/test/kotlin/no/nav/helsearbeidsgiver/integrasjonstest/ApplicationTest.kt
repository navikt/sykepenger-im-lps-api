package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.Producer
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselResponse
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingFilterResponse
import no.nav.helsearbeidsgiver.soknad.Sykepengesoknad
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.buildJournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import java.util.UUID

class ApplicationTest : LpsApiIntegrasjontest() {
    @Test
    fun `leser inntektsmelding fra kafka og henter det via api`() {
        val imId = UUID.randomUUID()
        val orgNr = "810007982"
        val imRecord =
            ProducerRecord(
                "helsearbeidsgiver.inntektsmelding",
                "key",
                buildJournalfoertInntektsmelding(
                    orgNr = Orgnr(orgNr),
                    inntektsmeldingId = imId,
                ),
            )
        Producer.sendMelding(imRecord)

        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmeldinger",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(orgNr),
                )

            val forespoerselSvar = response.body<InntektsmeldingFilterResponse>()
            forespoerselSvar.antall shouldBe 1
            forespoerselSvar.inntektsmeldinger[0].status shouldBe InnsendingStatus.GODKJENT
            forespoerselSvar.inntektsmeldinger[0].arbeidsgiver.orgnr shouldBe orgNr
            forespoerselSvar.inntektsmeldinger[0].id shouldBe imId
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

    @Test
    fun `leser søknad fra kafka og henter det via api`() {
        val soknadRecord = ProducerRecord("flex.sykepengesoknad", "key", TestData.SYKEPENGESOKNAD)
        Producer.sendMelding(soknadRecord)
        runTest {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/sykepengesoknad/9e088b5a-16c8-3dcc-91fb-acdd544b8607",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("315587336"),
                )
            val sykepengesoknad = response.body<Sykepengesoknad>()
            sykepengesoknad.fnr shouldBe "05449412615"
            sykepengesoknad.arbeidsgiver.orgnummer shouldBe "315587336"
        }
    }
}
