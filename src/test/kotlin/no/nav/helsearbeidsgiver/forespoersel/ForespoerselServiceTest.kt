package no.nav.helsearbeidsgiver.forespoersel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals

class ForespoerselServiceTest {
    private val forespoerselRepository = mockk<ForespoerselRepository>()
    private val forespoerselService = ForespoerselService(forespoerselRepository)

    @Test
    fun hentForespoerslerForOrgnr() {
        val forespoersler =
            getForespoerslerTestdata()

        every { forespoerselRepository.hentForespoersler(filter = ForespoerselFilter(orgnr = DEFAULT_ORG)) } returns forespoersler

        val response = forespoerselService.filtrerForespoersler(filter = ForespoerselFilter(orgnr = DEFAULT_ORG))
        assertEquals(2, response.size)

        verify { forespoerselRepository.hentForespoersler(filter = ForespoerselFilter(orgnr = DEFAULT_ORG)) }
    }

    @Test
    fun filtrerForespoersler() {
        val forespoersler = getForespoerslerTestdata()
        val request =
            ForespoerselFilter(
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
                navReferanseId = null,
                status = null,
            )

        every { forespoerselRepository.hentForespoersler(request) } returns forespoersler

        val response = forespoerselService.filtrerForespoersler(request)
        assertEquals(2, response.size)

        verify { forespoerselRepository.hentForespoersler(request) }
    }
}

private fun getForespoerslerTestdata(): List<Forespoersel> {
    val forespoersler =
        listOf(
            Forespoersel(
                loepenr = Random.nextLong(),
                navReferanseId = UUID.randomUUID(),
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
                status = Status.AKTIV,
                sykmeldingsperioder = emptyList(),
                egenmeldingsperioder = emptyList(),
                arbeidsgiverperiodePaakrevd = true,
                inntektPaakrevd = true,
                opprettetTid = LocalDateTime.now(),
            ),
            Forespoersel(
                loepenr = Random.nextLong(),
                navReferanseId = UUID.randomUUID(),
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
                status = Status.AKTIV,
                sykmeldingsperioder = emptyList(),
                egenmeldingsperioder = emptyList(),
                arbeidsgiverperiodePaakrevd = true,
                inntektPaakrevd = true,
                opprettetTid = LocalDateTime.now(),
            ),
        )
    return forespoersler
}
