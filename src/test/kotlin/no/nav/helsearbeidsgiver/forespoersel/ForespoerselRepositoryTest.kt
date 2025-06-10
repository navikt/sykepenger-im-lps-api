package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@WithPostgresContainer
@ExtendWith(TransactionalExtension::class)
class ForespoerselRepositoryTest {
    private lateinit var db: Database
    private lateinit var forespoerselRepository: ForespoerselRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        forespoerselRepository = configureRepositories(db).forespoerselRepository
    }

    @Test
    fun lagreOgOppdaterForespoersel() {
        val forespoerselID = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        forespoersler.size shouldBe 1
        forespoersler[0].status shouldBe Status.AKTIV
        forespoerselRepository.settBesvart(forespoerselID)
        forespoerselRepository.hentForespoersel(forespoerselID, DEFAULT_ORG)?.status shouldBe Status.BESVART
        forespoerselRepository.hentForespoersel(forespoerselID, DEFAULT_ORG.reversed()) shouldBe null
    }

    @Test
    fun hentVedtaksperiodeId() {
        val dokument = forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR)
        dokument.forespoerselId
        forespoerselRepository.lagreForespoersel(navReferanseId = dokument.forespoerselId, payload = dokument)
        forespoerselRepository.hentVedtaksperiodeId(dokument.forespoerselId) shouldBe dokument.vedtaksperiodeId
    }

    @Test
    fun lagreForespoerselDuplikat() {
        val forespoerselID = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        forespoersler.size shouldBe 1
    }

    @Test
    fun settForkastet() {
        val forespoerselID = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        forespoerselRepository.settForkastet(forespoerselID)
        forespoerselRepository.hentForespoersel(forespoerselID, DEFAULT_ORG)?.status shouldBe Status.FORKASTET
    }

    @Test
    fun hentForespoerslerForOrgnr() {
        val forespoerselID1 = UUID.randomUUID()
        val forespoerselID2 = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselID1, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        forespoerselRepository.lagreForespoersel(forespoerselID2, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        forespoersler.size shouldBe 2
    }

    @Test
    fun filtrerForespoersler() {
        val forespoerselID1 = UUID.randomUUID()
        val forespoerselID2 = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselID1, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        forespoerselRepository.lagreForespoersel(forespoerselID2, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        val request =
            ForespoerselRequest(
                fnr = DEFAULT_FNR,
                navReferanseId = null,
                status = null,
            )
        val forespoersler = forespoerselRepository.filtrerForespoersler(DEFAULT_ORG, request)
        forespoersler.size shouldBe 2
    }
}
