@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.refusjon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.uuid.Uuid

@Serializable
data class ArbeidstakerVedtakMelding(
    val eventName: SisEventName,
    @SerialName("fødselsnummer")
    val foedselsnummer: Fnr,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val organisasjonsnummer: Orgnr,
    val vedtaksperiodeId: Uuid,
    val behandlingId: Uuid,
    val fom: LocalDate,
    val tom: LocalDate,
    @SerialName("skjæringstidspunkt")
    val skjaeringstidspunkt: LocalDate,
    val dokumenter: List<Dokument>,
    val sykepengegrunnlag: Double,
    val anvendtSeksG: Double,
    val utbetalingsdager: List<Utbetalingsdag> = emptyList(),
    val vedtakFattetTidspunkt: LocalDateTime,
    val vedtaksUtfallTilArbeidsgiver: VedtaksUtfall,
    val saksbehandlerIdent: String?,
    val saksbehandlerNavn: String?,
    val beslutterIdent: String?,
    val beslutterNavn: String?,
    val automatiskFattet: Boolean,
    @SerialName("harArbeidsgiverØnsketRefusjon")
    val harArbeidsgiverOensketRefusjon: Boolean,
)

@Serializable
enum class SisEventName {
    @SerialName("vedtak_fattet")
    VEDTAK_FATTET,

    @SerialName("behandlingstatus")
    BEHANDLINGSTATUS,
}

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
    val dokumentId: Uuid,
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
    @Serializable
    val type: Utbetalingsdagtype,
    val sykdomsgrad: Int,
    val dekningsgrad: Int,
    @SerialName("beløpTilBruker")
    val beloepTilBruker: Int,
    @SerialName("beløpTilArbeidsgiver")
    val beloepTilArbeidsgiver: Int,
    val forespurtRefusjon: ForespurtRefusjon,
    val begrunnelser: List<String>,
)

@Serializable
data class ForespurtRefusjon(
    @SerialName("beløp")
    val beloep: Int,
    @Serializable
    val avsender: Avsender,
    val sasInntektsmeldingReferanse: Uuid? = null,
    val hagInntektsmeldingReferanse: Uuid? = null,
)

@Serializable
enum class Avsender {
    ARBEIDSGIVER,
    SAKSBEHANDLER,
    UKJENT,
}

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

    @SerialName("Permisjonsdag")
    PERMISJONSDAG,

    @SerialName("Arbeidsdag")
    ARBEIDSDAG,

    @SerialName("AvvistDag")
    AVVIST_DAG,

    @SerialName("ForeldetDag")
    FORELDET_DAG,

    @SerialName("Ventetidsdag")
    VENTETIDSDAG,

    @SerialName("ArbeidIkkeGjenopptattDag")
    ARBEID_IKKE_GJENOPPTATT_DAG,

    @SerialName("AndreYtelser")
    ANDRE_YTELSER,

    @SerialName("UkjentDag")
    UKJENT_DAG,
}
