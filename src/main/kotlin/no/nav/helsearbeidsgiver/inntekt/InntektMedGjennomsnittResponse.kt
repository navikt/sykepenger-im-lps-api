@file:UseSerializers(
    YearMonthSerializer::class,
)

package no.nav.helsearbeidsgiver.inntekt

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@Serializable
data class InntektMedGjennomsnittResponse private constructor(
    val inntektPerMaaned: Map<YearMonth, Double?>,
    val gjennomsnittAvMaaneder: Double,
) {
    companion object {
        fun of(inntekt: Map<YearMonth, Double?> = emptyMap()): InntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse(
                inntektPerMaaned = inntekt,
                gjennomsnittAvMaaneder = beregnGjennomsnitt(inntekt),
            )

        private fun beregnGjennomsnitt(inntekt: Map<YearMonth, Double?>): Double {
            if (inntekt.isEmpty()) return 0.0

            val sum =
                inntekt.values
                    .map { BigDecimal.valueOf(it?.takeIf(Double::isFinite) ?: 0.0) }
                    .fold(BigDecimal.ZERO, BigDecimal::add)

            return sum
                .divide(BigDecimal.valueOf(inntekt.size.toLong()), 2, RoundingMode.HALF_UP)
                .toDouble()
        }
    }
}
