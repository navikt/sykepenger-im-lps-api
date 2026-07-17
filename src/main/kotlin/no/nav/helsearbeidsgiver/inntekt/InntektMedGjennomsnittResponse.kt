@file:UseSerializers(
    YearMonthSerializer::class,
)

package no.nav.helsearbeidsgiver.inntekt

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.time.YearMonth

@Serializable
data class InntektMedGjennomsnittResponse private constructor(
    val inntektPerMaaned: Map<YearMonth, Double?>,
    val gjennomsnittAvMaaneder: Double,
) {
    companion object {
        fun of(inntektPerMaaned: Map<YearMonth, Double?> = emptyMap()): InntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse(
                inntektPerMaaned = inntektPerMaaned,
                gjennomsnittAvMaaneder = beregnGjennomsnitt(inntektPerMaaned),
            )

        private fun beregnGjennomsnitt(inntekt: Map<YearMonth, Double?>): Double =
            when {
                inntekt.isEmpty() -> 0.0
                inntekt.values.any { it == null || it == 0.0 } -> inntekt.values.filterNotNull().sum() / inntekt.size.toDouble()
                else -> inntekt.values.filterNotNull().average()
            }
    }
}
