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
}
