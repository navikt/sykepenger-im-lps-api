package no.nav.helsearbeidsgiver.csvimport

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CsvLeserTest {
    @Test
    fun tilbakestillForespoerslerTilStatusAktiv() {
        val forespoerselRepository: ForespoerselRepository = mockk()
        every { forespoerselRepository.oppdaterStatus(any(), Status.AKTIV) } returns 1
        val linjer =
            CsvLeser(
                ressurssti = "/excel-input/fsp_tilbakestilles.csv",
                forespoerselRepository = forespoerselRepository,
            ).tilbakestillForespoerslerTilStatusAktiv()
        assertEquals(19, linjer.size)
        verify(exactly = 19) { forespoerselRepository.oppdaterStatus(any(), Status.AKTIV) }
    }
}
