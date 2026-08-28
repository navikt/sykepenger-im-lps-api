@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.kafka.sis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class VedtakArbeidsgiverMelding(
    val eventName: SisEventName,
    @SerialName("fødselsnummer") val foedselsnummer: Fnr,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val organisasjonsnummer: Orgnr,
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    @SerialName("skjæringstidspunkt") val skjaeringstidspunkt: LocalDate,
    val dokumenter: List<Dokument>,
    val sykepengegrunnlag: Double,
    val utbetalingsdager: List<Utbetalingsdag> = emptyList(),
    val vedtakFattetTidspunkt: LocalDateTime,
    val vedtaksUtfallTilArbeidsgiver: VedtaksUtfall,
    val saksbehandlerIdent: String?,
    val saksbehandlerNavn: String?,
    val beslutterIdent: String?,
    val beslutterNavn: String?,
    val automatiskFattet: Boolean,
    @SerialName("harArbeidsgiverØnsketRefusjon") val harArbeidsgiverOensketRefusjon: Boolean,
)

@Serializable
enum class Yrkesaktivitetstype {
    ARBEIDSTAKER,
    SELVSTENDIG,
    FRILANS,
    ARBEIDSLEDIG,
}

@Serializable
enum class VedtaksUtfall {
    AVSLAG,
    DELVIS_INNVILGELSE,
    INNVILGELSE,
}

@Serializable
data class Dokument(
    val dokumentId: UUID,
    val type: Type,
) {
    @Serializable
    enum class Type {
        Sykmelding,

        @SerialName("Søknad")
        Soeknad,
        Inntektsmelding,
    }
}

@Serializable
data class Utbetalingsdag(
    val dato: LocalDate,
    val type: Utbetalingsdagtype,
    @SerialName("beløpTilArbeidsgiver") val beloepTilArbeidsgiver: Int,
)

@Serializable
enum class Utbetalingsdagtype {
    @SerialName("ArbeidsgiverperiodeDag")
    ARBEIDSGIVERPERIODE_DAG,

    @SerialName("ArbeidsgiverperiodedagNav")
    ARBEIDSGIVERPERIODEDAG_NAV,

    @SerialName("NavDag")
    NAV_DAG,

    @SerialName("NavHelgDag")
    NAV_HELG_DAG,

    @SerialName("Fridag")
    FRIDAG,

    @SerialName("Feriedag")
    FERIEDAG,

    @SerialName("Arbeidsdag")
    ARBEIDSDAG,

    @SerialName("AvvistDag")
    AVVIST_DAG,

    @SerialName("ForeldetDag")
    FORELDET_DAG,

    @SerialName("Ventetidsdag")
    VENTETIDSDAG,

    @SerialName("UkjentDag")
    UKJENT_DAG,
}
