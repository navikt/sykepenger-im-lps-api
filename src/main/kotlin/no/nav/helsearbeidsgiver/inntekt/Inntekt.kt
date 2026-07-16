@file:UseSerializers(
    YearMonthSerializer::class,
)

package no.nav.helsearbeidsgiver.inntekt

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.time.YearMonth

@Serializable
data class Inntekt(
    val inntekt: Map<YearMonth, Double?>,
    val gjennomsnittInntekt: Double =
        inntekt.values
            .filterNotNull()
            .average()
            .takeIf { it.isFinite() }
            ?: 0.0,
)
