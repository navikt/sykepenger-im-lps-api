package no.nav.helsearbeidsgiver.inntekt

import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlin.test.assertEquals

class InntektMedGjennomsnittResponseTest {
    @Test
    fun `beregner gjennomsnitt av verdier uten null`() {
        val inntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse.of(
                inntekt =
                    mapOf(
                        YearMonth.of(2024, 1) to 100.0,
                        YearMonth.of(2024, 2) to 200.0,
                        YearMonth.of(2024, 3) to 300.0,
                    ),
            )

        assertEquals(200.0, inntektMedGjennomsnittResponse.gjennomsnittAvMaaneder)
    }

    @Test
    fun `deler pa antall maneder når en verdi er null`() {
        val inntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse.of(
                inntekt =
                    mapOf(
                        YearMonth.of(2024, 1) to 100.0,
                        YearMonth.of(2024, 2) to null,
                        YearMonth.of(2024, 3) to 300.0,
                    ),
            )

        assertEquals(133.33, inntektMedGjennomsnittResponse.gjennomsnittAvMaaneder)
    }

    @Test
    fun `deler på antall maneder når en verdi er 0`() {
        val inntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse.of(
                inntekt =
                    mapOf(
                        YearMonth.of(2024, 1) to 100.0,
                        YearMonth.of(2024, 2) to 0.0,
                        YearMonth.of(2024, 3) to 300.0,
                    ),
            )

        assertEquals(133.33, inntektMedGjennomsnittResponse.gjennomsnittAvMaaneder)
    }

    @Test
    fun `deler på faktisk antall maneder når antall er ulikt 3`() {
        val inntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse.of(
                inntekt =
                    mapOf(
                        YearMonth.of(2024, 1) to 100.0,
                        YearMonth.of(2024, 2) to null,
                    ),
            )

        assertEquals(50.0, inntektMedGjennomsnittResponse.gjennomsnittAvMaaneder)
    }

    @Test
    fun `setter gjennomsnitt til 0 når alle verdier er null`() {
        val inntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse.of(
                inntekt =
                    mapOf(
                        YearMonth.of(2024, 1) to null,
                        YearMonth.of(2024, 2) to null,
                        YearMonth.of(2024, 3) to null,
                    ),
            )

        assertEquals(0.0, inntektMedGjennomsnittResponse.gjennomsnittAvMaaneder)
    }

    @Test
    fun `setter gjennomsnitt til 0 når map er tom`() {
        val inntektMedGjennomsnittResponse = InntektMedGjennomsnittResponse.of()

        assertEquals(0.0, inntektMedGjennomsnittResponse.gjennomsnittAvMaaneder)
    }
}
