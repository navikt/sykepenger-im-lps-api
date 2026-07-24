package no.nav.helsearbeidsgiver.inntekt

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InntektServiceTest {
    private val inntektKlient = mockk<InntektKlient>()
    private val inntektService = InntektService(inntektKlient)

    @BeforeEach
    fun resetMocks() {
        clearMocks(inntektKlient)
    }

    @Test
    fun `hentInntekter returnerer tre måneder og beregnet gjennomsnitt`() {
        val inntektsdato = LocalDate.of(2024, 4, 15)
        val fom = YearMonth.of(2024, 1)
        val middle = YearMonth.of(2024, 2)
        val tom = YearMonth.of(2024, 3)
        val forespoersel =
            mockForespoersel().copy(
                navReferanseId = UUID.randomUUID(),
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
            )

        coEvery {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = DEFAULT_FNR,
                fom = fom,
                tom = tom,
                navConsumerId = "helsearbeidsgiver-im-lps-api",
                callId = "helsearbeidsgiver-im-lps-api-${forespoersel.navReferanseId}",
            )
        } returns
            mapOf(
                DEFAULT_ORG to
                    mapOf(
                        fom to 100.0,
                        tom to 300.0,
                    ),
            )

        val result =
            runBlocking {
                inntektService.hentInntekter(forespoersel, inntektsdato)
            }
        coVerify(exactly = 1) {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = DEFAULT_FNR,
                fom = fom,
                tom = tom,
                navConsumerId = "helsearbeidsgiver-im-lps-api",
                callId = "helsearbeidsgiver-im-lps-api-${forespoersel.navReferanseId}",
            )
        }
        assertEquals(
            mapOf(
                fom to 100.0,
                middle to null,
                tom to 300.0,
            ),
            result.inntektPerMaaned,
        )
        assertEquals(133.33, result.gjennomsnittAvMaaneder)
    }

    @Test
    fun `hentInntekter kaster feil når inntektklient feiler`() {
        val inntektsdato = LocalDate.of(2024, 4, 15)
        val fom = YearMonth.of(2024, 1)
        val tom = YearMonth.of(2024, 3)
        val forespoersel =
            mockForespoersel().copy(
                navReferanseId = UUID.randomUUID(),
                orgnr = DEFAULT_ORG,
                fnr = DEFAULT_FNR,
            )

        coEvery {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = DEFAULT_FNR,
                fom = fom,
                tom = tom,
                navConsumerId = "helsearbeidsgiver-im-lps-api",
                callId = "helsearbeidsgiver-im-lps-api-${forespoersel.navReferanseId}",
            )
        } throws RuntimeException("inntektklient-feil")

        assertFailsWith<Exception> {
            runBlocking {
                inntektService.hentInntekter(forespoersel, inntektsdato)
            }
        }

        coVerify(exactly = 1) {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = DEFAULT_FNR,
                fom = fom,
                tom = tom,
                navConsumerId = "helsearbeidsgiver-im-lps-api",
                callId = "helsearbeidsgiver-im-lps-api-${forespoersel.navReferanseId}",
            )
        }
    }
}
