@file:UseSerializers(LocalDateSerializer::class, YearMonthSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.forespoersel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.time.LocalDate


@Serializable
data class ForespurtData(
    val arbeidsgiverperiode: Arbeidsgiverperiode,
    val inntekt: Inntekt,
    val refusjon: Refusjon
) {
    @Serializable
    data class Arbeidsgiverperiode(
        val paakrevd: Boolean
    )

    @Serializable
    data class Inntekt(
        val paakrevd: Boolean,
        val forslag: ForslagInntekt
    )

    @Serializable
    data class Refusjon(
        val paakrevd: Boolean,
        val forslag: ForslagRefusjon
    )
}

@Serializable
sealed class ForslagInntekt {
    @Serializable
    @SerialName("ForslagInntektGrunnlag")
    data class Grunnlag(
        val forrigeInntekt: ForrigeInntekt?
    ) : ForslagInntekt()

    @Serializable
    @SerialName("ForslagInntektFastsatt")
    data class Fastsatt(val fastsattInntekt: Double) : ForslagInntekt()
}

@Serializable
data class ForslagRefusjon(
    val perioder: List<Periode>,
    val opphoersdato: LocalDate?
) {
    @Serializable
    data class Periode(
        val fom: LocalDate,
        val beloep: Double
    )
}

@Serializable
data class ForrigeInntekt(
    val skjæringstidspunkt: LocalDate,
    val kilde: String,
    val beløp: Double
)
