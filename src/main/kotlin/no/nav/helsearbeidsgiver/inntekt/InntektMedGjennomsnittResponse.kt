@file:UseSerializers(
    YearMonthSerializer::class,
)

package no.nav.helsearbeidsgiver.inntekt

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@Serializable
data class InntektMedGjennomsnittResponse(
    val inntektPerMaaned: Map<YearMonth, Double?>,
) {
    @EncodeDefault
    val gjennomsnittAvMaaneder = beregnGjennomsnitt(inntektPerMaaned)

    private fun beregnGjennomsnitt(inntekt: Map<YearMonth, Double?>): Double {
        if (inntekt.isEmpty()) return 0.0

        val sum =
            inntekt.values
                .map { BigDecimal.valueOf(it ?: 0.0) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        return sum
            .divide(BigDecimal.valueOf(inntekt.size.toLong()), 2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
