package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Producer
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.soeknad.Sykepengesoeknad
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.buildForespoerselMottattJson
import no.nav.helsearbeidsgiver.utils.buildForespoerselOppdatertJson
import no.nav.helsearbeidsgiver.utils.buildJournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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

    @Test
    fun `leser oppdatert forespoersel fra kafka og henter det via api`() {
        val forespoerselId = UUID.randomUUID()
        val oppdatertForespoerselId = UUID.randomUUID()

        val forespoerselMottattJson = buildForespoerselMottattJson(forespoerselId = forespoerselId)

        val forespoerselOppdaterJson =
            buildForespoerselOppdatertJson(
                forespoerselId = oppdatertForespoerselId,
                eksponertForespoerselId = forespoerselId,
            )

        val priRecord = ProducerRecord(priTopic, "key", forespoerselMottattJson)
        val oppdatertPriRecord = ProducerRecord(priTopic, "key", forespoerselOppdaterJson)
        Producer.sendMelding(priRecord)
        Producer.sendMelding(oppdatertPriRecord)

        runBlocking {
            val responseOppdatertFsp =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersel/$oppdatertForespoerselId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            val oppdatertFsp = responseOppdatertFsp.body<Forespoersel>()
            val responseFsp =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersel/$forespoerselId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            val forespoerselSvar = responseFsp.body<Forespoersel>()

            forespoerselSvar.navReferanseId shouldBe forespoerselId
            oppdatertFsp.navReferanseId shouldBe oppdatertForespoerselId
            oppdatertFsp.status shouldBe Status.AKTIV
            forespoerselSvar.status shouldBe Status.FORKASTET

            transaction(db) {
                ForespoerselEntitet
                    .selectAll()
                    .where {
                        (ForespoerselEntitet.navReferanseId eq oppdatertForespoerselId) and
                            (ForespoerselEntitet.eksponertForespoerselId eq forespoerselId)
                    }.count() shouldBe 1
            }
        }
    }

    @Test
    fun `leser oppdatert forespoersel når den ikke finnes i db`() {
        val forespoerselId = UUID.randomUUID()
        val oppdatertForespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val forespoerselOppdaterJson =
            buildForespoerselOppdatertJson(
                forespoerselId = oppdatertForespoerselId,
                eksponertForespoerselId = forespoerselId,
                vedtaksperiodeId = vedtaksperiodeId,
            )

        val oppdatertPriRecord = ProducerRecord(priTopic, "key", forespoerselOppdaterJson)

        Producer.sendMelding(oppdatertPriRecord)

        sjekkOmForespoerselFinnesIDB(oppdatertForespoerselId, forespoerselId, vedtaksperiodeId)
    }

    @Test
    fun `leser oppdatert forespoersel når eksponert forespoersel er besvart`() {
        val forespoerselId = UUID.randomUUID()
        val oppdatertForespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val forespoerselMottattJson = buildForespoerselMottattJson(forespoerselId = forespoerselId, vedtaksperiodeId)
        val priMessage = jsonMapper.decodeFromString<PriMessage>(forespoerselMottattJson)
        val forespoersel = priMessage.forespoersel
        if (forespoersel != null) {
            services.forespoerselService.lagreNyForespoersel(
                forespoersel = forespoersel,
            )
            services.forespoerselService.settBesvart(forespoerselId)
        }

        val forespoerselOppdaterJson =
            buildForespoerselOppdatertJson(
                forespoerselId = oppdatertForespoerselId,
                eksponertForespoerselId = forespoerselId,
                vedtaksperiodeId = vedtaksperiodeId,
            )

        val oppdatertPriRecord = ProducerRecord(priTopic, "key", forespoerselOppdaterJson)

        Producer.sendMelding(oppdatertPriRecord)

        runBlocking {
            val responseOppdatertFsp =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersel/$oppdatertForespoerselId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            val oppdatertFsp = responseOppdatertFsp.body<Forespoersel>()
            oppdatertFsp.navReferanseId shouldBe oppdatertForespoerselId
            oppdatertFsp.status shouldBe Status.AKTIV

            transaction(db) {
                ForespoerselEntitet
                    .selectAll()
                    .where {
                        (ForespoerselEntitet.navReferanseId eq oppdatertForespoerselId) and
                            (ForespoerselEntitet.eksponertForespoerselId eq forespoerselId)
                    }.count() shouldBe 1
            }
            val responseFsp =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersel/$forespoerselId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            val forespoerselSvar = responseFsp.body<Forespoersel>()
            forespoerselSvar.navReferanseId shouldBe forespoerselId
            forespoerselSvar.status shouldBe Status.BESVART
        }
    }

    @Test
    fun `Avviser duplikat forespoersel`() {
        val oppdatertForespoerselId = UUID.randomUUID()
        val eksponertForespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val forespoerselMottattJson =
            buildForespoerselOppdatertJson(
                forespoerselId = oppdatertForespoerselId,
                eksponertForespoerselId = eksponertForespoerselId,
                vedtaksperiodeId = vedtaksperiodeId,
            )

        // Sender forespoersel til Kafka for første gang
        val priRecord = ProducerRecord(priTopic, "key", forespoerselMottattJson)
        Producer.sendMelding(priRecord)
        // Henter forespoersel fra db
        sjekkOmForespoerselFinnesIDB(oppdatertForespoerselId, eksponertForespoerselId, vedtaksperiodeId)
        // Sender samme forespoersel til Kafka på nytt
        Producer.sendMelding(priRecord)

        sjekkOmDetFinnesKunEnForespoerselIDB(oppdatertForespoerselId)
    }

    private fun sjekkOmForespoerselFinnesIDB(
        oppdatertForespoerselId: UUID,
        eksponertForespoerselId: UUID?,
        vedtaksperiodeId: UUID?,
    ) {
        runBlocking {
            val responseOppdatertFsp =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersel/$oppdatertForespoerselId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            val oppdatertFsp = responseOppdatertFsp.body<Forespoersel>()
            oppdatertFsp.navReferanseId shouldBe oppdatertForespoerselId
            oppdatertFsp.status shouldBe Status.AKTIV
            transaction(db) {
                ForespoerselEntitet
                    .selectAll()
                    .where {
                        (ForespoerselEntitet.navReferanseId eq oppdatertForespoerselId) and
                            (ForespoerselEntitet.eksponertForespoerselId eq eksponertForespoerselId) and
                            (ForespoerselEntitet.vedtaksperiodeId eq vedtaksperiodeId)
                    }.count() shouldBe 1
            }
        }
    }

    private fun sjekkOmDetFinnesKunEnForespoerselIDB(forespoerselId: UUID?) {
        runBlocking {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersler",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<List<Forespoersel>>()
            forespoerselSvar.size shouldBe 1
            forespoerselSvar.first().navReferanseId shouldBe forespoerselId
        }
    }
}
