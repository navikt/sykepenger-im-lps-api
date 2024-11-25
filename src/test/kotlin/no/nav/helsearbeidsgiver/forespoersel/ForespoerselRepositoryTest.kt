package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(TransactionalExtension::class)
class ForespoerselRepositoryTest {
    val db = Database.init()
    val forespoerselRepository = ForespoerselRepository(db)

    @Test
    fun lagreOgOppdaterForespoersel() {
        val forespoerselID = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        assertEquals(1, forespoersler.size)
        assertEquals(Status.AKTIV, forespoersler[0].status)
        forespoerselRepository.settBesvart(forespoerselID)
        assertEquals(Status.MOTTATT, forespoerselRepository.hentForespoersel(forespoerselID)?.status)
    }

    @Test
    fun lagreForespoerselDuplikat() {
        val forespoerselID = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        assertEquals(1, forespoersler.size)
    }

    @Test
    fun settForkastet() {
        val forespoerselID = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        forespoerselRepository.settForkastet(forespoerselID)
        assertEquals(Status.FORKASTET, forespoerselRepository.hentForespoersel(forespoerselID)?.status)
    }

    @Test
    fun hentForespoerslerForOrgnr() {
        val forespoerselID1 = UUID.randomUUID().toString()
        val forespoerselID2 = UUID.randomUUID().toString()
        forespoerselRepository.lagreForespoersel(forespoerselID1, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
        forespoerselRepository.lagreForespoersel(forespoerselID2, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))

        val forespoersler = forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
        assertEquals(2, forespoersler.size)
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
        assertEquals(2, forespoersler.size)
    }
}
