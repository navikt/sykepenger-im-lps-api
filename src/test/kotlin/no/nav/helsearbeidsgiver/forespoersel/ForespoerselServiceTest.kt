package no.nav.helsearbeidsgiver.forespoersel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.buildForespoerselOppdatertJson
import no.nav.helsearbeidsgiver.utils.getForespoerselFormJson
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals

class ForespoerselServiceTest {
    private val forespoerselRepository = mockk<ForespoerselRepository>(relaxed = true)
    private val dialogportenService = mockk<DialogportenService>()
    private val dokumentkoblingService = mockk<DokumentkoblingService>()
    private val forespoerselService = ForespoerselService(forespoerselRepository, dialogportenService, dokumentkoblingService)

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

    @Test
    fun `lagreOppdatertForespoersel forkaster eksisterende aktiv forespørsel`() {
        val forespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val eksponertForespoerselId = UUID.randomUUID()

        // Oppdarter forespørsel med agPaakrevd = false og inntektPaakrevd = true
        val forespoersel =
            buildForespoerselOppdatertJson(
                forespoerselId = forespoerselId,
                vedtaksperiodeId = vedtaksperiodeId,
                eksponertForespoerselId = eksponertForespoerselId,
                agPaakrevd = false,
                inntektPaakrevd = true,
            )

        val priMessage = getForespoerselFormJson(forespoersel)
        every { forespoerselRepository.hentForespoersel(forespoerselId) } returns null
        every { forespoerselRepository.hentIkkeForkastedeForespoerslerPaaVedtaksperiodeId(vedtaksperiodeId) } returns
            listOf(
                Forespoersel(
                    loepenr = Random.nextLong(),
                    navReferanseId = eksponertForespoerselId,
                    orgnr = DEFAULT_ORG,
                    fnr = DEFAULT_FNR,
                    status = Status.AKTIV,
                    sykmeldingsperioder = emptyList(),
                    egenmeldingsperioder = emptyList(),
                    inntektsdato = LocalDate.now(),
                    arbeidsgiverperiodePaakrevd = false,
                    inntektPaakrevd = false,
                    opprettetTid = LocalDateTime.now(),
                ),
            )
        forespoerselService.lagreOppdatertForespoersel(priMessage)
        verify { forespoerselRepository.hentForespoersel(forespoerselId) }
        verify { priMessage.forespoersel?.let { forespoerselRepository.lagreForespoersel(it, Status.AKTIV, any(), true, false) } }
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
                inntektsdato = LocalDate.now(),
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
                inntektsdato = LocalDate.now(),
                arbeidsgiverperiodePaakrevd = true,
                inntektPaakrevd = true,
                opprettetTid = LocalDateTime.now(),
            ),
        )
    return forespoersler
}
