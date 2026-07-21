package no.nav.helsearbeidsgiver.inntekt

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

private const val EPSILON = 1e-6

class InntektServiceTest {
    private val forespoerselRepository = mockk<ForespoerselRepository>()
    private val inntektKlient = mockk<InntektKlient>()
    private val inntektService = InntektService(forespoerselRepository, inntektKlient)

    @BeforeEach
    fun resetMocks() {
        clearMocks(forespoerselRepository, inntektKlient)
    }

    @Test
    fun `hentInntekter returnerer tre måneder og beregnet gjennomsnitt`() {
        val navReferanseId = UUID.randomUUID()
        val inntektsdato = LocalDate.of(2024, 4, 15)
        val fom = YearMonth.of(2024, 1)
        val middle = YearMonth.of(2024, 2)
        val tom = YearMonth.of(2024, 3)
        val forespoersel =
            mockForespoersel().copy(
                navReferanseId = navReferanseId,
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
            )

        every { forespoerselRepository.hentForespoersel(navReferanseId) } returns forespoersel
        coEvery {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = DEFAULT_FNR,
                fom = fom,
                tom = tom,
                navConsumerId = "helsearbeidsgiver-im-lps-api",
                callId = "helsearbeidsgiver-im-lps-api-$navReferanseId",
            )
        } returns
            mapOf(
                DEFAULT_ORG to
                    mapOf(
                        fom to 100.0,
                        tom to 300.0,
                    ),
            )

        val result = inntektService.hentInntekter(navReferanseId, inntektsdato)

        assertEquals(
            mapOf(
                fom to 100.0,
                middle to null,
                tom to 300.0,
            ),
            result.inntektPerMaaned,
        )
        assertEquals(133.33, result.gjennomsnittAvMaaneder, EPSILON)
    }

    @Test
    fun `hentInntekter kaster feil når inntektklient feiler`() {
        val navReferanseId = UUID.randomUUID()
        val inntektsdato = LocalDate.of(2024, 4, 15)
        val fom = YearMonth.of(2024, 1)
        val tom = YearMonth.of(2024, 3)
        val forespoersel =
            mockForespoersel().copy(
                navReferanseId = navReferanseId,
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
            )

        every { forespoerselRepository.hentForespoersel(navReferanseId) } returns forespoersel
        coEvery {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = DEFAULT_FNR,
                fom = fom,
                tom = tom,
                navConsumerId = "helsearbeidsgiver-im-lps-api",
                callId = "helsearbeidsgiver-im-lps-api-$navReferanseId",
            )
        } throws RuntimeException("inntektklient-feil")

        assertThrows<Exception> {
            inntektService.hentInntekter(navReferanseId, inntektsdato)
        }

        coVerify(exactly = 1) {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = DEFAULT_FNR,
                fom = fom,
                tom = tom,
                navConsumerId = "helsearbeidsgiver-im-lps-api",
                callId = "helsearbeidsgiver-im-lps-api-$navReferanseId",
            )
        }
    }
}
