@file:UseSerializers(OffsetDateTimeSerializer::class, UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.kafka.sis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.OffsetDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Vedtakmelding(
    @SerialName("fødselsnummer") val foedselsnummer: String,
    @SerialName("aktørId") val aktoerId: String,
    val organisasjonsnummer: String,
    val yrkesaktivitetstype: String, // ENUM?
    val fom: LocalDate,
    val tom: LocalDate,
    @SerialName("skjæringstidspunkt") val skjaeringstidspunkt: LocalDate,
    val dokumenter: List<Dokument>,
    val sykepengegrunnlag: Double,
    val utbetalingId: UUID,
    val vedtakFattetTidspunkt: LocalDateTime,
    val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    val begrunnelser: List<Begrunnelse> = emptyList(),
    val tags: List<String> = emptyList(),
    val saksbehandler: Saksbehandler? = null,
    val beslutter: Saksbehandler? = null,
    val forsikringsvurderingId: UUID? = null,
    val versjon: String,
    val begrensning: String, // Enum?
    val inntekt: Double,
    val grunnlagForSykepengegrunnlag: Double,
    val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
) {
    @Serializable
    data class Dokument(
        val dokumentId: UUID,
        val type: String,
    )

    @Serializable
    data class Sykepengegrunnlagsfakta(
        val fastsatt: String,
        @SerialName("omregnetÅrsinntekt") val omregnetAarsinntekt: Double? = null, // null for selvstendig?
        @SerialName("innrapportertÅrsinntekt") val innrapportertAarsinntekt: Double? = null,
        val avviksprosent: Double? = null,
        @SerialName("6G") val seksG: Double,
        val tags: List<String> = emptyList(),
        val arbeidsgivere: List<ArbeidsgiversInntekt> = emptyList(),
    )

    @Serializable
    data class ArbeidsgiversInntekt(
        val arbeidsgiver: String,
        @SerialName("omregnetÅrsinntekt") val omregnetAarsinntekt: Double,
    )

    @Serializable
    data class Begrunnelse(
        val type: String,
        val begrunnelse: String,
        val perioder: List<Periode>,
    )

    @Serializable
    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    @Serializable
    data class Saksbehandler(
        val navn: String,
        val ident: String,
    )
}
