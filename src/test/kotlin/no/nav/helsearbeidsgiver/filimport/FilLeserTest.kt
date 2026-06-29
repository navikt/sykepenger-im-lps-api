package no.nav.helsearbeidsgiver.filimport

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FilLeserTest {
    @Test
    fun tilbakestillForespoerslerTilStatusAktiv() {
        val forespoerselRepository: ForespoerselRepository = mockk()
        every { forespoerselRepository.oppdaterStatus(any(), Status.AKTIV) } returns 1
        val linjer =
            FilLeser(
                ressurssti = "/input/fsp_tilbakestilles.txt",
                forespoerselRepository = forespoerselRepository,
            ).tilbakestillForespoerslerTilStatusAktiv()
        assertEquals(19, linjer.size)
        verify(exactly = 19) { forespoerselRepository.oppdaterStatus(any(), Status.AKTIV) }
    }
}
