@file:UseSerializers(LocalDateSerializer::class, YearMonthSerializer::class, UuidSerializer::class)

package helsearbeidsgiver.nav.no.forespoersel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.bestemmendeFravaersdag
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Forespoersel(
    val type: ForespoerselType,
    val orgnr: String,
    val fnr: String,
    val vedtaksperiodeId: UUID,
    val sykmeldingsperioder: List<Periode>,
    val egenmeldingsperioder: List<Periode>,
    val bestemmendeFravaersdager: Map<String, LocalDate>,
    val forespurtData: ForespurtData,
    val erBesvart: Boolean
) {
    fun forslagBestemmendeFravaersdag(): LocalDate =
        bestemmendeFravaersdager[orgnr]
            ?: bestemmendeFravaersdag(
                arbeidsgiverperioder = emptyList(),
                sykmeldingsperioder = sykmeldingsperioder
            )

    fun forslagInntektsdato(): LocalDate =
        bestemmendeFravaersdager.minOfOrNull { it.value }
            ?: bestemmendeFravaersdag(
                arbeidsgiverperioder = emptyList(),
                sykmeldingsperioder = sykmeldingsperioder
            )

    fun eksternBestemmendeFravaersdag(): LocalDate? =
        bestemmendeFravaersdager.minus(orgnr).minOfOrNull { it.value }
}


enum class ForespoerselType {
    KOMPLETT,
    BEGRENSET,
    POTENSIELL
}

