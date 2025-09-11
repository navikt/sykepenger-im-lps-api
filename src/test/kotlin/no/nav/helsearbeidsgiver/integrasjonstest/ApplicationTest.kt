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
import no.nav.helsearbeidsgiver.innsending.InnsendingFeil
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.AvvistInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka.toJson
import no.nav.helsearbeidsgiver.soeknad.Sykepengesoeknad
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.buildForespoerselFraBacklog
import no.nav.helsearbeidsgiver.utils.buildForespoerselMottattJson
import no.nav.helsearbeidsgiver.utils.buildForespoerselOppdatertJson
import no.nav.helsearbeidsgiver.utils.buildForspoerselBesvartMelding
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.buildJournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
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
                    orgnr = Orgnr(orgnr),
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
    fun `leser inntektsmelding fra kafka og logger det uten å stoppe konsumeren`() {
        val inntektsmeldingId1 = UUID.randomUUID()
        val orgnr = "810007982"
        val imRecord1 =
            ProducerRecord(
                "helsearbeidsgiver.inntektsmelding",
                "key",
                buildJournalfoertInntektsmelding(
                    orgnr = Orgnr(orgnr),
                    inntektsmeldingId = inntektsmeldingId1,
                ),
            )
        val inntektsmeldingId2 = UUID.randomUUID()
        val imRecord2 =
            ProducerRecord(
                "helsearbeidsgiver.inntektsmelding",
                "key",
                buildJournalfoertInntektsmelding(
                    orgnr = Orgnr(orgnr),
                    inntektsmeldingId = inntektsmeldingId2,
                ),
            )
        Producer.sendMelding(imRecord1)
        Producer.sendMelding(imRecord1)
        Producer.sendMelding(imRecord2)
        runBlocking {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$inntektsmeldingId1",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr),
                )

            response.status.value shouldBe 200
        }
        runBlocking {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$inntektsmeldingId2",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(orgnr),
                )

            response.status.value shouldBe 200
        }
    }

    @Test
    fun `oppdaterer riktig inntektsmeldingstatus fra mottatt til feilet dersom vi mottar avvistevent`() {
        val inntektsmeldingId1 = UUID.randomUUID()
        val inntektsmeldingId2 = UUID.randomUUID()

        repositories.inntektsmeldingRepository.opprettInntektsmelding(
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId1),
            innsendingStatus = InnsendingStatus.MOTTATT,
        )
        repositories.inntektsmeldingRepository.opprettInntektsmelding(
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId2),
            innsendingStatus = InnsendingStatus.MOTTATT,
        )
        val avvistInntektsmelding =
            AvvistInntektsmelding(
                inntektsmeldingId = inntektsmeldingId2,
                feilkode = InnsendingFeil.Feilkode.INNTEKT_A_ORDNINGEN_AVVIK_MANGLER_AARSAK,
            )

        val melding =
            mapOf(
                InnsendingKafka.Key.EVENT_NAME to InnsendingKafka.EventName.AVVIST_INNTEKTSMELDING.toJson(),
                InnsendingKafka.Key.KONTEKST_ID to UUID.randomUUID().toJson(UuidSerializer),
                InnsendingKafka.Key.DATA to
                    mapOf(
                        InnsendingKafka.Key.AVVIST_INNTEKTSMELDING to
                            avvistInntektsmelding.toJson(
                                AvvistInntektsmelding.serializer(),
                            ),
                    ).toJson(),
            ).toJson().toString()

        Producer.sendMelding(ProducerRecord("helsearbeidsgiver.api-innsending", "key", melding))
        runBlocking {
            val response1 =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$inntektsmeldingId1",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG),
                )
            val response2 =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/inntektsmelding/$inntektsmeldingId2",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken(DEFAULT_ORG),
                )

            response1.body<InntektsmeldingResponse>().status shouldBe InnsendingStatus.MOTTATT
            response2.body<InntektsmeldingResponse>().status shouldBe InnsendingStatus.FEILET
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

        sendKafkaMelding(buildForespoerselMottattJson(forespoerselId))
        sendKafkaMelding(buildForespoerselOppdatertJson(oppdatertForespoerselId, forespoerselId))

        runBlocking {
            val oppdatertFsp = hentForespoerselFraApi(oppdatertForespoerselId)
            val forespoerselSvar = hentForespoerselFraApi(forespoerselId)

            forespoerselSvar.navReferanseId shouldBe forespoerselId
            oppdatertFsp.navReferanseId shouldBe oppdatertForespoerselId
            oppdatertFsp.status shouldBe Status.AKTIV
            forespoerselSvar.status shouldBe Status.FORKASTET

            sjekkForespoerselEntitetIDb(oppdatertForespoerselId, forespoerselId)
        }
    }

    @Test
    fun `leser oppdatert forespoersel når den ikke finnes i db`() {
        val forespoerselId = UUID.randomUUID()
        val oppdatertForespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        sendKafkaMelding(buildForespoerselOppdatertJson(oppdatertForespoerselId, forespoerselId, vedtaksperiodeId))
        sjekkForespoerselFinnesIDb(oppdatertForespoerselId, forespoerselId, vedtaksperiodeId)
    }

    @Test
    fun `leser oppdatert forespoersel når eksponert forespoersel er besvart`() {
        val forespoerselId = UUID.randomUUID()
        val oppdatertForespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val mottattJson = buildForespoerselMottattJson(forespoerselId, vedtaksperiodeId)
        val priMessage = jsonMapper.decodeFromString<PriMessage>(mottattJson)
        priMessage.forespoersel?.let {
            services.forespoerselService.lagreNyForespoersel(it)
            services.forespoerselService.settBesvart(forespoerselId)
        }

        sendKafkaMelding(buildForespoerselOppdatertJson(oppdatertForespoerselId, forespoerselId, vedtaksperiodeId))

        runBlocking {
            val oppdatertFsp = hentForespoerselFraApi(oppdatertForespoerselId)
            oppdatertFsp.navReferanseId shouldBe oppdatertForespoerselId
            oppdatertFsp.status shouldBe Status.AKTIV

            sjekkForespoerselEntitetIDb(oppdatertForespoerselId, forespoerselId)

            val forespoerselSvar = hentForespoerselFraApi(forespoerselId)
            forespoerselSvar.navReferanseId shouldBe forespoerselId
            forespoerselSvar.status shouldBe Status.BESVART
        }
    }

    @Test
    fun `Avviser duplikat forespoersel`() {
        val oppdatertForespoerselId = UUID.randomUUID()
        val eksponertForespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val json = buildForespoerselOppdatertJson(oppdatertForespoerselId, eksponertForespoerselId, vedtaksperiodeId)

        sendKafkaMelding(json)
        sjekkForespoerselFinnesIDb(oppdatertForespoerselId, eksponertForespoerselId, vedtaksperiodeId)
        sendKafkaMelding(json)
        sjekkKunEnForespoerselIDb(oppdatertForespoerselId)
    }

    @Test
    fun `Setter forespoersel som besvart`() {
        val forespoerselId = UUID.randomUUID()
        val oppdatertForespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        sendKafkaMelding(buildForespoerselMottattJson(forespoerselId, vedtaksperiodeId))
        sendKafkaMelding(buildForespoerselOppdatertJson(oppdatertForespoerselId, forespoerselId, vedtaksperiodeId))
        sendKafkaMelding(buildForspoerselBesvartMelding(oppdatertForespoerselId))

        runBlocking {
            val oppdatertFsp = hentForespoerselFraApi(oppdatertForespoerselId)
            oppdatertFsp.navReferanseId shouldBe oppdatertForespoerselId
            oppdatertFsp.status shouldBe Status.BESVART

            val forespoersel = hentForespoerselFraApi(forespoerselId)
            forespoersel.navReferanseId shouldBe forespoerselId
            forespoersel.status shouldBe Status.FORKASTET
        }
    }

    @Test
    fun `Sender flere oppdateringer på samme forespoersel`() {
        val forespoerselId = UUID.randomUUID()
        val oppdatertForespoerselId1 = UUID.randomUUID()
        val oppdatertForespoerselId2 = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        sendKafkaMelding(buildForespoerselMottattJson(forespoerselId, vedtaksperiodeId))
        sendKafkaMelding(buildForespoerselOppdatertJson(oppdatertForespoerselId1, forespoerselId, vedtaksperiodeId))
        sendKafkaMelding(buildForespoerselOppdatertJson(oppdatertForespoerselId2, forespoerselId, vedtaksperiodeId))
        sendKafkaMelding(buildForspoerselBesvartMelding(forespoerselId))
        runBlocking {
            val forespoersel = hentForespoerselFraApi(forespoerselId)
            forespoersel.status shouldBe Status.FORKASTET
            val oppdatertFsp1 = hentForespoerselFraApi(oppdatertForespoerselId1)
            oppdatertFsp1.status shouldBe Status.FORKASTET
            val oppdatertFsp2 = hentForespoerselFraApi(oppdatertForespoerselId2)
            oppdatertFsp2.status shouldBe Status.BESVART
        }
    }

    @Test
    fun `Mottar forespørsel fra backlog`() {
        val forespoerselId = UUID.randomUUID()
        sendKafkaMelding(buildForespoerselMottattJson(forespoerselId))
        runBlocking {
            val oppdatertFsp2 = hentForespoerselFraApi(forespoerselId)
            oppdatertFsp2.status shouldBe Status.AKTIV
        }
        val oppdatertForespoerselId1 = UUID.randomUUID()

        sendKafkaMelding(buildForespoerselFraBacklog(forespoerselId = forespoerselId, status = Status.FORKASTET))
        sendKafkaMelding(
            buildForespoerselFraBacklog(
                forespoerselId = oppdatertForespoerselId1,
                eksponertForespoerselId = forespoerselId,
                status = Status.AKTIV,
            ),
        )
        runBlocking {
            val oppdatertFsp1 = hentForespoerselFraApi(oppdatertForespoerselId1)
            oppdatertFsp1.status shouldBe Status.AKTIV
            val forespoersel = hentForespoerselFraApi(forespoerselId)
            forespoersel.status shouldBe Status.FORKASTET
        }
    }

    private fun sendKafkaMelding(json: String) {
        Producer.sendMelding(ProducerRecord(priTopic, "key", json))
    }

    private suspend fun hentForespoerselFraApi(forespoerselId: UUID): Forespoersel {
        val response =
            fetchWithRetry(
                url = "http://localhost:8080/v1/forespoersel/$forespoerselId",
                token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
            )
        return response.body()
    }

    private fun sjekkForespoerselEntitetIDb(
        navReferanseId: UUID,
        eksponertForespoerselId: UUID,
    ) {
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where {
                    (ForespoerselEntitet.navReferanseId eq navReferanseId) and
                        (ForespoerselEntitet.eksponertForespoerselId eq eksponertForespoerselId)
                }.count() shouldBe 1
        }
    }

    private fun sjekkForespoerselFinnesIDb(
        oppdatertForespoerselId: UUID,
        eksponertForespoerselId: UUID,
        vedtaksperiodeId: UUID,
    ) = runBlocking {
        val oppdatertFsp = hentForespoerselFraApi(oppdatertForespoerselId)
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

    private fun sjekkKunEnForespoerselIDb(forespoerselId: UUID?) =
        runBlocking {
            val response =
                fetchWithRetry(
                    url = "http://localhost:8080/v1/forespoersel/$forespoerselId",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<Forespoersel>()
            forespoerselSvar.navReferanseId shouldBe forespoerselId
        }
}
