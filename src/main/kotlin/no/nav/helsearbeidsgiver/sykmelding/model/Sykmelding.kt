@file:UseSerializers(
    LocalDateSerializer::class,
    LocalDateTimeSerializer::class,
)

package no.nav.helsearbeidsgiver.sykmelding.model

import io.ktor.openapi.JsonSchema.Description
import io.ktor.openapi.JsonSchema.Format
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
@Description("SykmeldingArbeidsgiver")
data class Sykmelding(
    val loepenr: Long,
    @Format("uuid")
    val sykmeldingId: String,
    val mottattAvNav: LocalDateTime,
    val sendtTilArbeidsgiver: LocalDateTime,
    val sykmeldt: Sykmeldt,
    val egenmeldingsdager: List<Periode>,
    @Description("Når startet syketilfellet")
    val sykefravaerFom: LocalDate?,
    @Description("Sammenhengende, ikke overlappende perioder for denne sykmeldingen")
    val sykmeldingPerioder: List<SykmeldingPeriode>,
    val oppfoelging: Oppfoelging,
    @Description("Ved å oppgi informasjonen nedenfor bekreftes at personen er kjent eller har vist legitimasjon")
    val kontaktMedPasient: LocalDateTime,
    val behandler: Behandler?,
    val arbeidsgiver: SykmeldingArbeidsgiver,
)

@Serializable
@Description("Sykmeldingsperiode")
data class SykmeldingPeriode(
    @Description("Sykmeldingsperiodens fra og med dato")
    val fom: LocalDate,
    @Description("Sykmeldingsperiodens til og med dato")
    val tom: LocalDate,
    @Description("Om arbeidsrelatert aktivitet er mulig i perioden")
    val aktivitet: Aktivitet,
)

@Serializable
@Description("Aktivitet")
data class Aktivitet(
    val avventendeSykmelding: String?,
    val gradertSykmelding: GradertSykmelding?,
    val aktivitetIkkeMulig: AktivitetIkkeMulig?,
    val antallBehandlingsdagerUke: Int?,
    val harReisetilskudd: Boolean,
)

@Serializable
@Description("Gradert sykmelding")
data class GradertSykmelding(
    @Description("Angitt sykemeldingsgrad")
    val sykmeldingsgrad: Int,
    @Description("Reisetilskudd ved gradert sykmelding")
    val harReisetilskudd: Boolean,
)

@Serializable
@Description("Aktivitet ikke mulig")
data class AktivitetIkkeMulig(
    @Description("Settes til true dersom arbeidsplassen mangler tilrettelegging")
    val manglendeTilretteleggingPaaArbeidsplassen: Boolean,
    @Description("Eventuell beskrivelse på hvorfor aktivitet ikke er mulig")
    val beskrivelse: String? = null,
)

@Serializable
data class Oppfoelging(
    val prognose: Prognose? = null,
    @Description("Øvrige kommentarer: kontakt mellom lege/arbeidsgiver - melding fra behandler")
    val meldingTilArbeidsgiver: String? = null,
    @Description("Innspill til tiltak som kan bedre arbeidsevnen")
    val tiltakArbeidsplassen: String? = null,
)

@Serializable
data class Prognose(
    @Description("Arbeidsfør etter denne perioden?")
    val erArbeidsfoerEtterEndtPeriode: Boolean,
    @Description("Hvis arbeidsfør etter denne perioden: Beskriv eventuelle hensyn som må tas på arbeidsplassen.")
    val beskrivHensynArbeidsplassen: String? = null,
)

@Serializable
data class Behandler(
    val navn: String,
    val tlf: String,
)

@Serializable
data class SykmeldingArbeidsgiver(
    @Description("Navn på arbeidsgiver slik det fremkommer av sykmeldingen. Dette navnet fylles ut av lege.")
    val navn: String? = null,
    @Description("Orgnr for underenheten i bedriften den sykmeldte er knyttet til")
    val orgnr: Orgnr,
)

@Serializable
data class Sykmeldt(
    val fnr: Fnr,
    val navn: String,
)
