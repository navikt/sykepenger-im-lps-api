@file:UseSerializers(
    LocalDateSerializer::class,
    LocalDateTimeSerializer::class,
)

package no.nav.helsearbeidsgiver.sykmelding.model

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
@Schema(description = "SykmeldingArbeidsgiver")
data class SykmeldingArbeidsgiver(
    @field:Schema(description = "Sykmeldingen")
    val sykmelding: Sykmelding,
    @field:Schema(description = "Juridisk organisasjonsnummer for den bedriften den sykmeldte er knyttet til")
    val juridiskOrganisasjonsnummer: Long,
    @field:Schema(description = "Virksomhetsnummer (undernummer/bedriftsnummer) for den bedriften den sykmeldte er knyttet til")
    val virksomhetsnummer: Long,
    @field:Schema(description = "Sykmeldingens unike id")
    val sykmeldingId: String,
    @field:Schema(description = "Dato og tid for når sykmeldingen ble mottatt hos NAV")
    val mottattidspunkt: LocalDateTime,
    val egenmeldingsdager: Egenmeldingsdager? = null,
)

@Serializable
@Schema(description = "Sykmelding")
data class Sykmelding(
    @field:Schema(description = "Når startet syketilfellet")
    val syketilfelleFom: LocalDate?,
    @field:Schema(description = "Pasient")
    val pasient: Pasient,
    @field:Schema(description = "Arbeidsgiver oppgitt av behandler")
    val arbeidsgiver: Arbeidsgiver? = null,
    @field:Schema(description = "Sammenhengende, ikke overlappende perioder for denne sykmeldingen")
    val perioder: List<Periode>? = null,
    @field:Schema(description = "Prognose")
    val prognose: Prognose? = null,
    @field:Schema(description = "Innspill til tiltak som kan bedre arbeidsevnen")
    val tiltak: Tiltak? = null,
    @field:Schema(description = "Øvrige kommentarer: kontakt mellom lege/arbeidsgiver - melding fra behandler")
    val meldingTilArbeidsgiver: String? = null,
    val kontaktMedPasient: KontaktMedPasient,
    val behandler: Behandler,
)

@Serializable
@Schema(description = "Periode")
data class Periode(
    @field:Schema(description = "Sykmeldingsperiodens fra og med dato")
    val fom: LocalDate,
    @field:Schema(description = "Sykmeldingsperiodens til og med dato")
    val tom: LocalDate,
    @field:Schema(description = "Om arbeidsrelatert aktivitet er mulig i perioden")
    val aktivitet: Aktivitet,
)

@Serializable
@Schema(description = "Aktivitet")
data class Aktivitet(
    @Schema(description = "Avventende sykmelding")
    val avventendeSykmelding: String?,
    @Schema(description = "Gradert sykmelding")
    val gradertSykmelding: GradertSykmelding?,
    @Schema(description = "Aktivitet ikke mulig")
    val aktivitetIkkeMulig: AktivitetIkkeMulig?,
    @Schema(description = "Antall behandlingsdager per uke")
    val antallBehandlingsdagerUke: Int?,
    @Schema(description = "Har reisetilskudd")
    val harReisetilskudd: Boolean?,
)

@Serializable
@Schema(description = "Gradert sykmelding")
data class GradertSykmelding(
    @field:Schema(description = "Angitt sykemeldingsgrad")
    val sykmeldingsgrad: Int,
    @field:Schema(description = "Reisetilskudd ved gradert sykmelding")
    val harReisetilskudd: Boolean? = null,
)

@Serializable
@Schema(description = "Aktivitet ikke mulig")
data class AktivitetIkkeMulig(
    @field:Schema(description = "Settes til true dersom arbeidsplassen mangler tilrettelegging")
    val manglendeTilretteleggingPaaArbeidsplassen: Boolean? = null,
    @field:Schema(description = "Eventuell beskrivelse på hvorfor aktivitet ikke er mulig")
    val beskrivelse: String? = null,
)

@Serializable
@Schema(description = "Arbeidsutsikter")
data class Arbeidsutsikter(
    @field:Schema(description = "Gjelder de MED arbeidsgiver")
    val harEgetArbeidPaaSikt: Boolean? = null,
    @field:Schema(description = "Gjelder de UTEN arbeidsgiver")
    val arbeidFom: String? = null,
    val harAnnetArbeidPaaSikt: Boolean? = null,
)

@Serializable
@Schema(description = "Prognose")
data class Prognose(
    @field:Schema(description = "Arbeidsfør etter denne perioden?")
    val erArbeidsfoerEtterEndtPeriode: Boolean? = null,
    @field:Schema(description = "Hvis arbeidsfør etter denne perioden: Beskriv eventuelle hensyn som må tas på arbeidsplassen.")
    val beskrivHensynArbeidsplassen: String? = null,
    @field:Schema(description = "Utsikter for arbeid")
    val arbeidsutsikter: Arbeidsutsikter? = null,
)

@Serializable
@Schema(description = "Innspill til tiltak som kan bedre arbeidsevnen")
data class Tiltak(
    val tiltakArbeidsplassen: String? = null,
)

@Serializable
@Schema(description = "Kontakt med pasient")
data class KontaktMedPasient(
    @field:Schema(description = "Ved å oppgi informasjonen nedenfor bekreftes at personen er kjent eller har vist legitimasjon")
    val behandlet: LocalDateTime,
)

@Serializable
@Schema(description = "Navn")
data class Navn(
    @field:Schema(description = "Etternavn")
    val etternavn: String,
    @field:Schema(description = "Mellomnavn")
    val mellomnavn: String? = null,
    @field:Schema(description = "Fornavn")
    val fornavn: String,
)

@Serializable
@Schema(description = "Pasient")
data class Pasient(
    @field:Schema(description = "Pasientens navn")
    val navn: Navn,
    @field:Schema(description = "Pasientens fnr/dnr")
    val ident: String,
)

@Serializable
@Schema(description = "Behandler")
data class Behandler(
    @field:Schema(description = "Behandlers navn")
    val navn: Navn,
    @field:Schema(description = "Behandlers kontaktinformasjon")
    val telefonnummer: Long,
)

@Serializable
@Schema(description = "Arbeidsgiver")
data class Arbeidsgiver(
    @field:Schema(description = "Navn på arbeidsgiver slik det fremkommer av sykmeldingen. Dette navnet fylles ut av lege.")
    val navn: String? = null,
)

@Serializable
@Schema(description = "Egenmeldingsdager")
data class Egenmeldingsdager(
    val dager: List<LocalDate>?,
)
