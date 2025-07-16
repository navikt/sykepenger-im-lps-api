package no.nav.helsearbeidsgiver.integrasjonstest

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Producer
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.testcontainer.LpsApiIntegrasjontest
import no.nav.helsearbeidsgiver.utils.buildForespoerselMottattJson
import no.nav.helsearbeidsgiver.utils.buildForespoerselOppdatertJson
import no.nav.helsearbeidsgiver.utils.buildForspoerselBesvartMelding
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.util.UUID

class ForespoerselKafkaTest : LpsApiIntegrasjontest() {
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
        eksponertForespoerselId: UUID?,
        vedtaksperiodeId: UUID?,
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
                    url = "http://localhost:8080/v1/forespoersler",
                    token = mockOAuth2Server.gyldigSystembrukerAuthToken("810007842"),
                )
            response.status.value shouldBe 200
            val forespoerselSvar = response.body<List<Forespoersel>>()
            forespoerselSvar.size shouldBe 1
            forespoerselSvar.first().navReferanseId shouldBe forespoerselId
        }
}
