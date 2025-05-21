@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.soknad

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Datamodell for Sykepengesøknad.
 * Inneholder bare felter som skal vises til arbeidsgiver.
 * Er hentet fra flex sin applikasjon for å sende søknader til arbeidsgiver via altinn
 * https://github.com/navikt/sykepengesoknad-altinn/tree/main/src/main/kotlin/no/nav/syfo/domain/soknad
 */

@Serializable
data class Sykepengesoknad(
    val id: UUID,
    val fnr: String,
    val sykmeldingId: UUID? = null,
    val type: Soknadstype,
    // val status: Soknadsstatus,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val startSykeforlop: LocalDate? = null,
    val arbeidGjenopptatt: LocalDate? = null,
    // val opprettet: LocalDateTime? = null,
    val sendtNav: LocalDateTime? = null,
    val sendtArbeidsgiver: LocalDateTime? = null,
    val sykmeldingSkrevet: LocalDateTime? = null,
    val arbeidsgiver: Arbeidsgiver,
    val arbeidsgiverForskutterer: ArbeidsverForskutterer,
    val soktUtenlandsopphold: Boolean? = null,
    val korrigerer: String? = null,
    // val korrigertAv: String? = null,
    // val arbeidssituasjon: Arbeidssituasjon? = null,
    val soknadsperioder: List<Soknadsperiode> = arrayListOf(),
    // val behandlingsdager: List<LocalDate> = arrayListOf(),
    val egenmeldinger: List<Periode> = arrayListOf(),
    val fravarForSykmeldingen: List<Periode> = arrayListOf(),
    // val papirsykmeldinger: List<Periode> = arrayListOf(),
    val fravar: List<Fravar> = arrayListOf(),
    // val andreInntektskilder: List<Inntektskilde> = arrayListOf(),
    val sporsmal: List<Sporsmal> = arrayListOf(),
    val avsendertype: Avsendertype? = null,
    // val ettersending: Boolean = false,
) {
    @Serializable
    enum class Soknadstype {
        SELVSTENDIGE_OG_FRILANSERE,
        OPPHOLD_UTLAND,
        ARBEIDSTAKERE,
        BEHANDLINGSDAGER,
        GRADERT_REISETILSKUDD,
    }

    /*@Serializable
    enum class Soknadsstatus {
        NY,
        SENDT,
        FREMTIDIG,
        UTKAST_TIL_KORRIGERING,
        KORRIGERT,
        AVBRUTT,
        SLETTET,
    }*/

    @Serializable
    data class Arbeidsgiver(
        val navn: String,
        val orgnummer: String,
    )

    @Serializable
    enum class ArbeidsverForskutterer {
        JA,
        NEI,
        VET_IKKE,
        IKKE_SPURT, // Default verdi når arbeidsgiverForskutterer ikke er satt
    }

    /*@Serializable
    enum class Arbeidssituasjon(
        val value: String,
    ) {
        NAERINGSDRIVENDE("selvstendig næringsdrivende"),
        FRILANSER("frilanser"),
        ARBEIDSTAKER("arbeidstaker"),
        ;

        override fun toString() = value
    }*/

    @Serializable
    data class Soknadsperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val sykmeldingsgrad: Int,
        val faktiskGrad: Int? = null,
        val avtaltTimer: Double? = null,
        val faktiskTimer: Double? = null,
        val sykmeldingstype: Sykmeldingstype? = null,
    )

    @Serializable
    enum class Sykmeldingstype {
        AKTIVITET_IKKE_MULIG,
        GRADERT,
        BEHANDLINGSDAGER,
        AVVENTENDE,
        REISETILSKUDD,
    }

    /*@Serializable
    data class Inntektskilde(
        val type: Inntektskildetype,
        val sykmeldt: Boolean?,
    )

    @Serializable
    enum class Inntektskildetype {
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
    }*/

    @Serializable
    data class Fravar(
        val fom: LocalDate,
        val tom: LocalDate?,
        val type: Fravarstype,
    )

    @Serializable
    enum class Fravarstype {
        FERIE,
        PERMISJON,
        UTLANDSOPPHOLD,
        UTDANNING_FULLTID,
        UTDANNING_DELTID,
        UKJENT,
    }

    @Serializable
    enum class Avsendertype {
        BRUKER,
        SYSTEM,
    }

    @Serializable
    data class Sporsmal(
        val id: String? = null,
        val tag: String? = null,
        val sporsmalstekst: String? = null,
        val undertekst: String? = null,
        val svartype: Svartype? = null,
        val min: String? = null,
        val max: String? = null,
        val kriterieForVisningAvUndersporsmal: Visningskriterium? = null,
        val svar: List<Svar> = arrayListOf(),
        val undersporsmal: List<Sporsmal> = arrayListOf(),
    )

    @Serializable
    enum class Visningskriterium {
        JA,
        NEI,
        CHECKED,
    }

    @Serializable
    data class Svar(
        val verdi: String? = null,
    )

    @Serializable
    enum class Svartype {
        JA_NEI,
        CHECKBOX,
        CHECKBOX_GRUPPE,
        CHECKBOX_PANEL,
        DATO,
        PERIODE,
        PERIODER,
        TIMER,
        FRITEKST,
        LAND,
        IKKE_RELEVANT,
        PROSENT,
        RADIO_GRUPPE,
        RADIO_GRUPPE_TIMER_PROSENT,
        RADIO_GRUPPE_UKEKALENDER,
        RADIO,
        TALL,
        INFO_BEHANDLINGSDAGER,
        KVITTERING,
        DATOER,
        BELOP,
        KILOMETER,
        BEKREFTELSESPUNKTER,
        OPPSUMMERING,
    }
}
