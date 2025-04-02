package no.nav.helsearbeidsgiver.forespoersel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class ForespoerselServiceTest {
    private val forespoerselRepository = mockk<ForespoerselRepository>()
    private val forespoerselService = ForespoerselService(forespoerselRepository)

    @Test
    fun hentForespoerslerForOrgnr() {
        val forespoersler =
            getForespoerslerTestdata()

        every { forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG) } returns forespoersler

        val response = forespoerselService.hentForespoerslerForOrgnr(DEFAULT_ORG)
        assertEquals(2, response.antall)

        verify { forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG) }
    }

    @Test
    fun filtrerForespoerslerForOrgnr() {
        val forespoersler = getForespoerslerTestdata()
        val request =
            ForespoerselRequest(
                fnr = DEFAULT_FNR,
                forespoerselId = null,
                status = null,
            )

        every { forespoerselRepository.filtrerForespoersler(DEFAULT_ORG, request) } returns forespoersler

        val response = forespoerselService.filtrerForespoerslerForOrgnr(DEFAULT_ORG, request)
        assertEquals(2, response.antall)

        verify { forespoerselRepository.filtrerForespoersler(DEFAULT_ORG, request) }
    }
}

private fun getForespoerslerTestdata(): List<Forespoersel> {
    val forespoersler =
        listOf(
            Forespoersel(
                forespoerselId = UUID.randomUUID(),
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
                status = Status.AKTIV,
                sykmeldingsperioder = emptyList(),
                egenmeldingsperioder = emptyList(),
                arbeidsgiverperiodePaakrevd = true,
                inntektPaakrevd = true,
            ),
            Forespoersel(
                forespoerselId = UUID.randomUUID(),
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
                status = Status.AKTIV,
                sykmeldingsperioder = emptyList(),
                egenmeldingsperioder = emptyList(),
                arbeidsgiverperiodePaakrevd = true,
                inntektPaakrevd = true,
            ),
        )
    return forespoersler
}
