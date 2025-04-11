@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding.model

import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.ArbeidsgiverAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.BehandlerAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.PrognoseAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import java.time.LocalDate
import java.time.OffsetDateTime

fun tilSykmeldingArbeidsgiver(
    sykmeldingDTO: SendSykmeldingAivenKafkaMessage,
    person: Person,
): SykmeldingArbeidsgiver =
    SykmeldingArbeidsgiver(
        juridiskOrganisasjonsnummer = 0,
        mottattidspunkt =
            sykmeldingDTO.sykmelding.mottattTidspunkt
                .toLocalDateTime(),
        sykmeldingId = sykmeldingDTO.sykmelding.id,
        virksomhetsnummer =
            sykmeldingDTO.event.arbeidsgiver.orgnummer
                .toLong(),
        sykmelding = sykmeldingDTO.sykmelding.tilSykmelding(person),
        egenmeldingsdager = sykmeldingDTO.event.sporsmals.tilEgenmeldingsdager(),
    )

private fun List<SykmeldingStatusKafkaEventDTO.SporsmalOgSvarDTO>?.tilEgenmeldingsdager(): Egenmeldingsdager? =
    this
        ?.find { it.shortName == SykmeldingStatusKafkaEventDTO.ShortNameDTO.EGENMELDINGSDAGER }
        ?.let { Json.decodeFromString<List<String>>(it.svar) }
        ?.map { LocalDate.parse(it) }
        ?.let { Egenmeldingsdager(it) }

private fun ArbeidsgiverSykmeldingKafka.tilSykmelding(person: Person): Sykmelding =
    Sykmelding(
        arbeidsgiver = arbeidsgiver.tilArbeidsgiver(),
        behandler = behandler.tilBehandler(),
        kontaktMedPasient = behandletTidspunkt.tilKontaktMedPasient(),
        meldingTilArbeidsgiver = meldingTilArbeidsgiver,
        pasient = person.tilPasient(),
        perioder = sykmeldingsperioder.tilPerioderAG(),
        prognose = prognose?.getPrognose(),
        syketilfelleFom = syketilfelleStartDato,
        tiltak = tiltakArbeidsplassen?.tilTiltak(),
    )

private fun String.tilTiltak(): Tiltak = Tiltak(tiltakArbeidsplassen = this)

private fun PrognoseAGDTO.getPrognose(): Prognose =
    Prognose(
        erArbeidsfoerEtterEndtPeriode = this.arbeidsforEtterPeriode,
        beskrivHensynArbeidsplassen = this.hensynArbeidsplassen,
    )

private fun List<SykmeldingsperiodeAGDTO>.tilPerioderAG(): List<Periode> =
    map {
        Periode(
            fom = it.fom,
            tom = it.tom,
            aktivitet = it.tilAktivitet(),
        )
    }

private fun SykmeldingsperiodeAGDTO.tilAktivitet(): Aktivitet =
    Aktivitet(
        avventendeSykmelding = innspillTilArbeidsgiver,
        gradertSykmelding = gradert?.let { GradertSykmelding(it.grad, it.reisetilskudd) },
        aktivitetIkkeMulig = aktivitetIkkeMulig?.tilAktivitetIkkeMulig(),
        harReisetilskudd = if (this.reisetilskudd) true else null,
        antallBehandlingsdagerUke = behandlingsdager,
    )

private fun SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO.tilAktivitetIkkeMulig(): AktivitetIkkeMulig =
    AktivitetIkkeMulig(
        manglendeTilretteleggingPaaArbeidsplassen = erMangledneTilrettelegging(this),
        beskrivelse = this.arbeidsrelatertArsak?.beskrivelse,
    )

private fun erMangledneTilrettelegging(aktivitetIkkeMulig: SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO): Boolean? =
    aktivitetIkkeMulig.arbeidsrelatertArsak?.arsak?.any {
        it ==
            SykmeldingsperiodeAGDTO
                .AktivitetIkkeMuligAGDTO
                .ArbeidsrelatertArsakDTO
                .ArbeidsrelatertArsakTypeDTO
                .MANGLENDE_TILRETTELEGGING
    }

private fun Person.tilPasient(): Pasient =
    Pasient(
        navn =
            Navn(
                fornavn = fornavn,
                mellomnavn = mellomnavn,
                etternavn = etternavn,
            ),
        ident = fnr,
    )

private fun OffsetDateTime.tilKontaktMedPasient(): KontaktMedPasient = KontaktMedPasient(behandlet = this.toLocalDateTime())

private fun BehandlerAGDTO?.tilBehandler(): Behandler =
    Behandler(
        navn = tilNavn(),
        telefonnummer = this?.tlf.tolkTelefonNr().toLong(),
    )

fun String?.tolkTelefonNr(): String =
    this
        ?.lowercase()
        ?.trim()
        ?.removePrefix("tel:")
        ?.removePrefix("fax:") ?: ""

private fun BehandlerAGDTO?.tilNavn(): Navn =
    Navn(
        fornavn = this?.fornavn ?: "",
        etternavn = this?.etternavn ?: "",
        mellomnavn = this?.mellomnavn ?: "",
    )

private fun ArbeidsgiverAGDTO.tilArbeidsgiver(): Arbeidsgiver =
    Arbeidsgiver(
        navn = this.navn,
    )
