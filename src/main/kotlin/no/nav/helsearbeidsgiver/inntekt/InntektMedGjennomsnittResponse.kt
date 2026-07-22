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
            val normaliserteVerdier = inntekt.values.map { it?.takeIf(Double::isFinite) }

            return avrundTilToDesimaler(
                when {
                    inntekt.isEmpty() -> {
                        0.0
                    }

                    normaliserteVerdier.any { it == null || it == 0.0 } -> {
                        normaliserteVerdier.filterNotNull().sum() /
                            inntekt.size.toDouble()
                    }

                    else -> {
                        normaliserteVerdier.filterNotNull().average()
                    }
                },
            )
        }

        private fun avrundTilToDesimaler(value: Double): Double =
            if (value.isFinite()) {
                BigDecimal
                    .valueOf(value)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
            } else {
                0.0
            }
    }
}
