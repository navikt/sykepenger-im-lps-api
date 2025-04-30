@file:UseSerializers(
    LocalDateSerializer::class,
    LocalDateTimeSerializer::class,
)

package no.nav.helsearbeidsgiver.sykmelding.model

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykmeldt
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
@Schema(description = "SykmeldingArbeidsgiver")
data class Sykmelding(
    val sykmeldingId: String,
    val mottattAvNav: LocalDateTime,
    val sykmeldt: Sykmeldt,
    val egenmeldingsdager: List<Periode>,
    @field:Schema(description = "Når startet syketilfellet")
    val sykefravaerFom: LocalDate?,
    @field:Schema(description = "Sammenhengende, ikke overlappende perioder for denne sykmeldingen")
    val sykmeldingPerioder: List<SykmeldingPeriode>,
    val oppfoelging: Oppfoelging,
    @field:Schema(description = "Ved å oppgi informasjonen nedenfor bekreftes at personen er kjent eller har vist legitimasjon")
    val kontaktMedPasient: LocalDateTime,
    val behandler: Behandler?,
    val arbeidsgiver: Arbeidsgiver,
)

@Serializable
data class SykmeldingPeriode(
    @field:Schema(description = "Sykmeldingsperiodens fra og med dato")
    val fom: LocalDate,
    @field:Schema(description = "Sykmeldingsperiodens til og med dato")
    val tom: LocalDate,
    @field:Schema(description = "Om arbeidsrelatert aktivitet er mulig i perioden")
    val aktivitet: Aktivitet,
)

@Serializable
data class Aktivitet(
    val avventendeSykmelding: String?,
    val gradertSykmelding: GradertSykmelding?,
    val aktivitetIkkeMulig: AktivitetIkkeMulig?,
    val antallBehandlingsdagerUke: Int?,
    val harReisetilskudd: Boolean,
)

@Serializable
data class GradertSykmelding(
    @field:Schema(description = "Angitt sykemeldingsgrad")
    val sykmeldingsgrad: Int,
    @field:Schema(description = "Reisetilskudd ved gradert sykmelding")
    val harReisetilskudd: Boolean,
)

@Serializable
data class AktivitetIkkeMulig(
    @field:Schema(description = "Settes til true dersom arbeidsplassen mangler tilrettelegging")
    val manglendeTilretteleggingPaaArbeidsplassen: Boolean,
    @field:Schema(description = "Eventuell beskrivelse på hvorfor aktivitet ikke er mulig")
    val beskrivelse: String? = null,
)

@Serializable
data class Oppfoelging(
    val prognose: Prognose? = null,
    @field:Schema(description = "Øvrige kommentarer: kontakt mellom lege/arbeidsgiver - melding fra behandler")
    val meldingTilArbeidsgiver: String? = null,
    @Schema(description = "Innspill til tiltak som kan bedre arbeidsevnen")
    val tiltakArbeidsplassen: String? = null,
)

@Serializable
data class Prognose(
    @field:Schema(description = "Arbeidsfør etter denne perioden?")
    val erArbeidsfoerEtterEndtPeriode: Boolean,
    @field:Schema(description = "Hvis arbeidsfør etter denne perioden: Beskriv eventuelle hensyn som må tas på arbeidsplassen.")
    val beskrivHensynArbeidsplassen: String? = null,
)

@Serializable
data class Behandler(
    val navn: String,
    val tlf: String,
)

@Serializable
data class Arbeidsgiver(
    @field:Schema(description = "Navn på arbeidsgiver slik det fremkommer av sykmeldingen. Dette navnet fylles ut av lege.")
    val navnFraBehandler: String? = null,
    @field:Schema(description = "Orgnr for overenheten i bedriften den sykmeldte er knyttet til")
    val orgnrHovedenhet: Orgnr?,
    @field:Schema(description = "Orgnr for underenheten i bedriften den sykmeldte er knyttet til")
    val orgnr: Orgnr,
)
