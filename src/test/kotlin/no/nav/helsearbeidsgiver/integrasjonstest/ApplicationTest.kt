package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Producer
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.soeknad.Sykepengesoeknad
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
    fun `helsesjekk sier ok`() {
        val response = runBlocking { client.get("http://localhost:8080/health/is-alive") }
        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `readyness sjekk sier ok`() {
        val response = runBlocking { client.get("http://localhost:8080/health/is-ready") }
        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `leser inntektsmelding fra kafka og henter det via api`() {
        val inntektsmeldingId = UUID.randomUUID()
        val orgnr = "810007982"
        val imRecord =
            ProducerRecord(
                "helsearbeidsgiver.inntektsmelding",
                "key",
                buildJournalfoertInntektsmelding(
                    orgNr = Orgnr(orgnr),
                    inntektsmeldingId = inntektsmeldingId,
                ),
            )
        Producer.sendMelding(imRecord)

        runBlocking {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$inntektsmeldingId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr),
                )

            val imSvar = response.body<InntektsmeldingResponse>()
            imSvar.id shouldBe inntektsmeldingId
            imSvar.status shouldBe InnsendingStatus.GODKJENT
            imSvar.arbeidsgiver.orgnr shouldBe orgnr
        }
    }

    @Test
    fun `leser forespoersel fra kafka og henter det via api`() {
        val priRecord = ProducerRecord("helsearbeidsgiver.pri", "key", TestData.FORESPOERSEL_MOTTATT)
        Producer.sendMelding(priRecord)
        val id = "c8d75a15-dce3-4db2-8b48-fc4d9a1cfd5c"
        runBlocking {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersel/$id",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007982"),
                )
            val forespoerselSvar = response.body<Forespoersel>()
            forespoerselSvar.navReferanseId shouldBe UUID.fromString(id)
        }
    }

    @Test
    fun `leser søknad fra kafka og henter det via api`() {
        val soeknadRecord = ProducerRecord("flex.sykepengesoknad", "key", TestData.SYKEPENGESOEKNAD)
        Producer.sendMelding(soeknadRecord)
        runBlocking {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/sykepengesoeknad/9e088b5a-16c8-3dcc-91fb-acdd544b8607",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("315587336"),
                )
            val sykepengesoeknad = response.body<Sykepengesoeknad>()
            sykepengesoeknad.fnr shouldBe "05449412615"
            sykepengesoeknad.arbeidsgiver.orgnr shouldBe "315587336"
        }
    }

    @Test
    fun `leser søknad og oppdaterer vedtaksperiodeId fra kafka `() {
        val soeknadId = UUID.fromString("9e088b5a-16c8-3dcc-91fb-acdd544b8607")
        val vedtaksperiodeId = UUID.fromString("3e377f98-1801-4fd2-8d14-cf95d2b831fa")
        val soeknadRecord = ProducerRecord("flex.sykepengesoknad", "key", TestData.SYKEPENGESOEKNAD)
        val sisRecord = ProducerRecord("tbd.sis", "key", TestData.STATUS_I_SPLEIS_MELDING)

        Producer.sendMelding(soeknadRecord)
        Producer.sendMelding(sisRecord)

        // Sikre at søknaden og status i speil er lagret før vi sjekker koblingen mellom søknad og vedtaksperiode
        var antallRetries = 0
        while (antallRetries < 5) {
            val soeknad = repositories.soeknadRepository.hentSoeknad(soeknadId)
            val soeknadIder = repositories.statusISpeilRepository.hentSoeknadIderForVedtaksperiodeId(vedtaksperiodeId)
            if (soeknad != null && soeknadIder.isNotEmpty()) {
                break
            } else {
                Thread.sleep(100)
                antallRetries++
            }
        }
        val soeknadListe =
            repositories.soeknadRepository.hentSoeknaderMedVedtaksperiodeId(vedtaksperiodeId).map { it.id }
        soeknadListe shouldBe listOf(soeknadId)
    }
}
