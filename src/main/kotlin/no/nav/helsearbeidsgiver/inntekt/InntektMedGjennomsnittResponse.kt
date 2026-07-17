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
    val inntekt: Map<YearMonth, Double?>,
    val gjennomsnittInntekt: Double,
) {
    companion object {
        fun of(inntekt: Map<YearMonth, Double?> = emptyMap()): InntektMedGjennomsnittResponse =
            InntektMedGjennomsnittResponse(
                inntekt = inntekt,
                gjennomsnittInntekt = beregnGjennomsnitt(inntekt),
            )

        private fun beregnGjennomsnitt(inntekt: Map<YearMonth, Double?>): Double =
            inntekt.values
                .filterNotNull()
                .average()
                .takeIf { it.isFinite() }
                ?: 0.0
    }
}
