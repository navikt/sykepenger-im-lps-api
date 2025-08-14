package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
import java.time.LocalDate
import java.time.LocalDateTime
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
        forespoerselRepository.lagreForespoersel(
            forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID),
        )
        val forespoersler = forespoerselRepository.hentForespoersler(DEFAULT_ORG)
        forespoersler.size shouldBe 1
        forespoersler[0].status shouldBe Status.AKTIV
        forespoerselRepository.oppdaterStatus(forespoerselID, Status.BESVART)
        forespoerselRepository.hentForespoersel(forespoerselID, DEFAULT_ORG)?.status shouldBe Status.BESVART
        forespoerselRepository.hentForespoersel(forespoerselID, DEFAULT_ORG.reversed()) shouldBe null
    }

    @Test
    fun hentVedtaksperiodeId() {
        val dokument = forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR)
        dokument.forespoerselId
        forespoerselRepository.lagreForespoersel(dokument, Status.AKTIV)
        forespoerselRepository.hentVedtaksperiodeId(dokument.forespoerselId) shouldBe dokument.vedtaksperiodeId
    }

    @Test
    fun settForkastet() {
        val forespoerselID = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID))

        forespoerselRepository.oppdaterStatus(forespoerselID, Status.FORKASTET)
        forespoerselRepository.hentForespoersel(forespoerselID, DEFAULT_ORG)?.status shouldBe Status.FORKASTET
    }

    @Test
    fun hentForespoerslerForOrgnr() {
        val forespoerselID1 = UUID.randomUUID()
        val forespoerselID2 = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID1))
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID2))

        val forespoersler = forespoerselRepository.hentForespoersler(DEFAULT_ORG)
        forespoersler.size shouldBe 2
    }

    @Test
    fun filtrerForespoersler() {
        val forespoerselID1 = UUID.randomUUID()
        val forespoerselID2 = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID1))
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID2))

        val request =
            ForespoerselRequest(
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
                navReferanseId = null,
                status = null,
            )
        val forespoersler = forespoerselRepository.hentForespoersler(request)
        forespoersler.size shouldBe 2
    }

    @Test
    fun filtrerForespoerslerPåOpprettetTid() {
        val now = LocalDateTime.of(2023, 12, 31, 23, 59)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns now // LocalDateTime.now() brukes ved lagreForespørsel
        val forespoerselID1 = UUID.randomUUID()
        val forespoerselID2 = UUID.randomUUID()
        val forespoerselID3 = UUID.randomUUID()
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID1))
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID2))
        every { LocalDateTime.now() } returns now.plusDays(1)
        forespoerselRepository.lagreForespoersel(forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselID3))
        val request =
            ForespoerselRequest(
                orgnr = DEFAULT_ORG,
                fom = now.toLocalDate(),
            )
        val forespoersler = forespoerselRepository.hentForespoersler(request)
        forespoersler.size shouldBe 3
        val request2 = ForespoerselRequest(orgnr = DEFAULT_ORG, fom = now.toLocalDate().plusDays(1))
        forespoerselRepository.hentForespoersler(request2).size shouldBe 1
        val request3 = ForespoerselRequest(orgnr = DEFAULT_ORG, fom = now.toLocalDate().plusDays(2))
        forespoerselRepository.hentForespoersler(request3).size shouldBe 0

        val requestTom = ForespoerselRequest(orgnr = DEFAULT_ORG, tom = now.toLocalDate())
        forespoerselRepository.hentForespoersler(requestTom).size shouldBe 2

        val requestForTidlig = ForespoerselRequest(orgnr = DEFAULT_ORG, tom = now.toLocalDate().minusDays(1))
        forespoerselRepository.hentForespoersler(requestForTidlig).size shouldBe 0
        shouldThrow<IllegalArgumentException> {
            ForespoerselRequest(orgnr = DEFAULT_ORG, tom = LocalDate.MAX)
        }
        shouldThrow<IllegalArgumentException> {
            ForespoerselRequest(orgnr = DEFAULT_ORG, fom = LocalDate.of(-1, 12, 12))
        }
        unmockkStatic(LocalDateTime::class)
    }
}
