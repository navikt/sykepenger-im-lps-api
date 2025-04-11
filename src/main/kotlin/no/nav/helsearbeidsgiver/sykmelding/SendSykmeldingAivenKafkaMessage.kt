@file:UseSerializers(LocalDateSerializer::class, OffsetDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.OffsetDateTimeSerializer
import java.time.LocalDate
import java.time.OffsetDateTime

@Serializable
data class SendSykmeldingAivenKafkaMessage(
    val sykmelding: ArbeidsgiverSykmeldingKafka,
    val kafkaMetadata: KafkaMetadataDTO,
    val event: SykmeldingStatusKafkaEventDTO,
)

@Serializable
data class ArbeidsgiverSykmeldingKafka(
    val id: String,
    val mottattTidspunkt: OffsetDateTime,
    val syketilfelleStartDato: LocalDate? = null,
    val behandletTidspunkt: OffsetDateTime,
    val arbeidsgiver: ArbeidsgiverAGDTO,
    val sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>,
    val prognose: PrognoseAGDTO? = null,
    val tiltakArbeidsplassen: String? = null,
    val meldingTilArbeidsgiver: String? = null,
    val kontaktMedPasient: KontaktMedPasientAGDTO,
    val behandler: BehandlerAGDTO? = null,
    val egenmeldt: Boolean,
    val papirsykmelding: Boolean,
    val harRedusertArbeidsgiverperiode: Boolean,
    val merknader: List<Merknad>? = null,
    val utenlandskSykmelding: UtenlandskSykmeldingAGDTO? = null,
    val signaturDato: OffsetDateTime? = null,
) {
    @Serializable
    data class ArbeidsgiverAGDTO(
        val navn: String? = null,
        val yrkesbetegnelse: String? = null,
    )

    @Serializable
    data class SykmeldingsperiodeAGDTO(
        val fom: LocalDate,
        val tom: LocalDate,
        val gradert: GradertDTO? = null,
        val behandlingsdager: Int? = null,
        val innspillTilArbeidsgiver: String? = null,
        val type: PeriodetypeDTO,
        val aktivitetIkkeMulig: AktivitetIkkeMuligAGDTO? = null,
        val reisetilskudd: Boolean,
    ) {
        @Serializable
        data class GradertDTO(
            val grad: Int,
            val reisetilskudd: Boolean,
        )

        @Serializable
        enum class PeriodetypeDTO {
            AKTIVITET_IKKE_MULIG,
            AVVENTENDE,
            BEHANDLINGSDAGER,
            GRADERT,
            REISETILSKUDD,
        }

        @Serializable
        data class AktivitetIkkeMuligAGDTO(
            val arbeidsrelatertArsak: ArbeidsrelatertArsakDTO? = null,
        ) {
            @Serializable
            data class ArbeidsrelatertArsakDTO(
                val beskrivelse: String? = null,
                val arsak: List<ArbeidsrelatertArsakTypeDTO>,
            ) {
                @Serializable
                enum class ArbeidsrelatertArsakTypeDTO(
                    val codeValue: String,
                    val text: String,
                    val oid: String = "2.16.578.1.12.4.1.1.8132",
                ) {
                    MANGLENDE_TILRETTELEGGING("1", "Manglende tilrettelegging på arbeidsplassen"),
                    ANNET("9", "Annet"),
                }
            }
        }
    }

    @Serializable
    data class PrognoseAGDTO(
        val arbeidsforEtterPeriode: Boolean,
        val hensynArbeidsplassen: String? = null,
    )

    @Serializable
    data class KontaktMedPasientAGDTO(
        val kontaktDato: LocalDate? = null,
    )

    @Serializable
    data class BehandlerAGDTO(
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val hpr: String? = null,
        val adresse: AdresseDTO,
        val tlf: String? = null,
    ) {
        @Serializable
        data class AdresseDTO(
            val gate: String? = null,
            val postnummer: Int? = null,
            val kommune: String? = null,
            val postboks: String? = null,
            val land: String? = null,
        )
    }

    @Serializable
    data class Merknad(
        val type: String,
        val beskrivelse: String? = null,
    )

    @Serializable
    data class UtenlandskSykmeldingAGDTO(
        val land: String,
    )
}

@Serializable
data class KafkaMetadataDTO(
    val sykmeldingId: String,
    val timestamp: OffsetDateTime,
    val fnr: String,
    val source: String,
)

@Serializable
data class SykmeldingStatusKafkaEventDTO(
    val sykmeldingId: String,
    val timestamp: OffsetDateTime,
    val statusEvent: String,
    val arbeidsgiver: ArbeidsgiverStatusDTO,
    val sporsmals: List<SporsmalOgSvarDTO>? = null,
    val erSvarOppdatering: Boolean? = null,
    val tidligereArbeidsgiver: TidligereArbeidsgiverDTO? = null,
) {
    @Serializable
    data class ArbeidsgiverStatusDTO(
        val orgnummer: String,
        val juridiskOrgnummer: String? = null,
        val orgNavn: String,
    )

    @Serializable
    data class SporsmalOgSvarDTO(
        val tekst: String,
        val shortName: ShortNameDTO,
        val svartype: SvartypeDTO,
        val svar: String,
    )

    @Serializable
    enum class ShortNameDTO {
        ARBEIDSSITUASJON,
        NY_NARMESTE_LEDER,
        FRAVAER,
        PERIODE,
        FORSIKRING,
        EGENMELDINGSDAGER,
    }

    @Serializable
    enum class SvartypeDTO {
        ARBEIDSSITUASJON,
        PERIODER,
        JA_NEI,
        DAGER,
    }

    @Serializable
    data class TidligereArbeidsgiverDTO(
        val orgNavn: String,
        val orgnummer: String,
        val sykmeldingsId: String,
    )
}
