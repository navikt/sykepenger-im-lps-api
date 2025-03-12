package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(TransactionalExtension::class)
class ForespoerselRepositoryTest {
    val db = DbConfig.init()
    val forespoerselRepository = ForespoerselRepository(db)

    @Test
    fun lagreOgOppdaterForespoersel() {
        val forespoerselID = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        forespoersler.size shouldBe 1
        forespoersler[0].status shouldBe Status.AKTIV
        forespoerselRepository.settBesvart(forespoerselID)
        forespoerselRepository.hentForespoersel(forespoerselID)?.status shouldBe Status.MOTTATT
    }

    @Test
    fun lagreForespoerselDuplikat() {
        val forespoerselID = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        forespoersler.size shouldBe 1
    }

    @Test
    fun settForkastet() {
        val forespoerselID = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        forespoerselRepository.settForkastet(forespoerselID)
        forespoerselRepository.hentForespoersel(forespoerselID)?.status shouldBe Status.FORKASTET
    }

    @Test
    fun hentForespoerslerForOrgnr() {
        val forespoerselID1 = UUID.randomUUID().toString()
        val forespoerselID2 = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID1, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        forespoerselRepository.lagreForespoersel(forespoerselID2, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        forespoersler.size shouldBe 2
    }

    @Test
    fun filtrerForespoersler() {
        val forespoerselID1 = UUID.randomUUID().toString()
        val forespoerselID2 = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID1, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        forespoerselRepository.lagreForespoersel(forespoerselID2, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        val request =
            ForespoerselRequest(
                fnr = DEFAULT_FNR,
                forespoerselId = null,
                status = null,
            )
        val forespoersler = forespoerselRepository.filtrerForespoersler(DEFAULT_ORG, request)
        forespoersler.size shouldBe 2
    }
}
