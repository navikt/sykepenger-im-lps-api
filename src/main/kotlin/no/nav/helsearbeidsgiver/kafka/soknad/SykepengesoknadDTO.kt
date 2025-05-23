@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka.soknad

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class SykepengesoknadDTO(
    val id: UUID,
    val type: SoknadstypeDTO,
    val status: SoknadsstatusDTO,
    val fnr: String,
    val sykmeldingId: UUID? = null,
    val arbeidsgiver: ArbeidsgiverDTO? = null,
    val arbeidssituasjon: ArbeidssituasjonDTO? = null,
    val korrigerer: UUID? = null, // gjør om til uuid?
    val korrigertAv: String? = null,
    val soktUtenlandsopphold: Boolean? = null,
    val arbeidsgiverForskutterer: ArbeidsgiverForskuttererDTO? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val dodsdato: LocalDate? = null,
    val startSyketilfelle: LocalDate? = null,
    val arbeidGjenopptatt: LocalDate? = null,
    val friskmeldt: LocalDate? = null,
    val sykmeldingSkrevet: LocalDateTime? = null,
    val opprettet: LocalDateTime? = null,
    val opprinneligSendt: LocalDateTime? = null,
    val sendtNav: LocalDateTime? = null,
    val sendtArbeidsgiver: LocalDateTime? = null,
    val egenmeldinger: List<PeriodeDTO>? = null,
    val fravarForSykmeldingen: List<PeriodeDTO>? = null,
    val papirsykmeldinger: List<PeriodeDTO>? = null,
    val fravar: List<FravarDTO>? = null,
    val andreInntektskilder: List<InntektskildeDTO>? = null,
    val soknadsperioder: List<SoknadsperiodeDTO>? = null,
    val sporsmal: List<SporsmalDTO>? = null,
    val avsendertype: AvsendertypeDTO? = null,
    val ettersending: Boolean = false,
    val mottaker: MottakerDTO? = null,
    val egenmeldtSykmelding: Boolean? = null,
    val yrkesskade: Boolean? = null,
    val arbeidUtenforNorge: Boolean? = null,
    val harRedusertVenteperiode: Boolean? = null,
    val behandlingsdager: List<LocalDate>? = null,
    val permitteringer: List<PeriodeDTO>? = null,
    val merknaderFraSykmelding: List<MerknadDTO>? = null,
    val egenmeldingsdagerFraSykmelding: List<LocalDate>? = null,
    val merknader: List<String>? = null,
    val sendTilGosys: Boolean? = null,
    val utenlandskSykmelding: Boolean? = null,
    val medlemskapVurdering: String? = null,
    val forstegangssoknad: Boolean? = null,
    val tidligereArbeidsgiverOrgnummer: String? = null,
    val fiskerBlad: FiskerBladDTO? = null,
    val inntektFraNyttArbeidsforhold: List<InntektFraNyttArbeidsforholdDTO>? = null,
    val friskTilArbeidVedtakId: String? = null,
    val friskTilArbeidVedtakPeriode: String? = null,
    val fortsattArbeidssoker: Boolean? = null,
    val inntektUnderveis: Boolean? = null,
    val ignorerArbeidssokerregister: Boolean? = null,
) {
    @Serializable
    enum class SoknadstypeDTO {
        SELVSTENDIGE_OG_FRILANSERE,
        OPPHOLD_UTLAND,
        ARBEIDSTAKERE,
        ANNET_ARBEIDSFORHOLD,
        ARBEIDSLEDIG,
        BEHANDLINGSDAGER,
        REISETILSKUDD,
        GRADERT_REISETILSKUDD,
        FRISKMELDT_TIL_ARBEIDSFORMIDLING,
    }

    @Serializable
    enum class SoknadsstatusDTO {
        NY,
        SENDT,
        FREMTIDIG,
        KORRIGERT,
        AVBRUTT,
        SLETTET,
        UTGAATT,
    }

    @Serializable
    data class ArbeidsgiverDTO(
        val navn: String? = null,
        val orgnummer: String? = null,
    )

    @Serializable
    enum class ArbeidssituasjonDTO {
        SELVSTENDIG_NARINGSDRIVENDE,
        FISKER,
        JORDBRUKER,
        FRILANSER,
        ARBEIDSTAKER,
        ARBEIDSLEDIG,
        ANNET,
    }

    @Serializable
    enum class ArbeidsgiverForskuttererDTO {
        JA,
        NEI,
        VET_IKKE,
    }

    @Serializable
    data class PeriodeDTO(
        val fom: LocalDate? = null,
        val tom: LocalDate? = null,
    )

    @Serializable
    data class FravarDTO(
        val fom: LocalDate? = null,
        val tom: LocalDate? = null,
        val type: FravarstypeDTO? = null,
    )

    @Serializable
    enum class FravarstypeDTO {
        FERIE,
        PERMISJON,
        UTLANDSOPPHOLD,
        UTDANNING_FULLTID,
        UTDANNING_DELTID,
    }

    @Serializable
    data class InntektskildeDTO(
        val type: InntektskildetypeDTO? = null,
        val sykmeldt: Boolean? = null,
    )

    @Serializable
    enum class InntektskildetypeDTO {
        ANDRE_ARBEIDSFORHOLD,
        FRILANSER,
        SELVSTENDIG_NARINGSDRIVENDE,
        SELVSTENDIG_NARINGSDRIVENDE_DAGMAMMA,
        JORDBRUKER_FISKER_REINDRIFTSUTOVER,
        ANNET,
        FOSTERHJEMGODTGJORELSE,
        OMSORGSLONN,
        ARBEIDSFORHOLD,
        FRILANSER_SELVSTENDIG,
        STYREVERV,
    }

    @Serializable
    data class SoknadsperiodeDTO(
        val fom: LocalDate? = null,
        val tom: LocalDate? = null,
        val sykmeldingsgrad: Int? = null,
        val faktiskGrad: Int? = null,
        val avtaltTimer: Double? = null,
        val faktiskTimer: Double? = null,
        val sykmeldingstype: SykmeldingstypeDTO? = null,
        // Fra gammel SoknadDTO. Bør konsolideres med sykmeldingsgrad.
        val grad: Int? = null,
    )

    @Serializable
    enum class SykmeldingstypeDTO {
        AKTIVITET_IKKE_MULIG,
        GRADERT,
        BEHANDLINGSDAGER,
        AVVENTENDE,
        REISETILSKUDD,
    }

    @Serializable
    data class SporsmalDTO(
        val id: String? = null,
        val tag: String? = null,
        val sporsmalstekst: String? = null,
        val undertekst: String? = null,
        val min: String? = null,
        val max: String? = null,
        val svartype: SvartypeDTO? = null,
        val kriterieForVisningAvUndersporsmal: VisningskriteriumDTO? = null,
        val svar: List<SvarDTO>? = null,
        val undersporsmal: List<SporsmalDTO>? = null,
    )

    @Serializable
    enum class SvartypeDTO {
        JA_NEI,
        CHECKBOX,
        CHECKBOX_GRUPPE,
        CHECKBOX_PANEL,
        DATO,
        PERIODE,
        PERIODER,
        TIMER,
        FRITEKST,
        IKKE_RELEVANT,
        GRUPPE_AV_UNDERSPORSMAL,
        BEKREFTELSESPUNKTER,
        OPPSUMMERING,
        PROSENT,
        RADIO_GRUPPE,
        RADIO_GRUPPE_TIMER_PROSENT,
        RADIO,
        TALL,
        RADIO_GRUPPE_UKEKALENDER,
        LAND,
        COMBOBOX_SINGLE,
        COMBOBOX_MULTI,
        INFO_BEHANDLINGSDAGER,
        KVITTERING,
        DATOER,
        BELOP,
        KILOMETER,
    }

    @Serializable
    enum class VisningskriteriumDTO {
        NEI,
        JA,
        CHECKED,
    }

    @Serializable
    data class SvarDTO(
        val verdi: String? = null,
    )

    @Serializable
    enum class AvsendertypeDTO {
        BRUKER,
        SYSTEM,
    }

    @Serializable
    enum class MottakerDTO {
        NAV,
        ARBEIDSGIVER,
        ARBEIDSGIVER_OG_NAV,
    }

    @Serializable
    data class MerknadDTO(
        val type: String,
        val beskrivelse: String? = null,
    )

    @Serializable
    enum class FiskerBladDTO {
        A,
        B,
    }

    @Serializable
    data class InntektFraNyttArbeidsforholdDTO(
        val fom: LocalDate,
        val tom: LocalDate,
        val harJobbet: Boolean,
        val belop: Int?,
        val arbeidsstedOrgnummer: String,
        val opplysningspliktigOrgnummer: String,
    )
}
