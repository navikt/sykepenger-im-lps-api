@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding.model

import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.ArbeidsgiverAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.BehandlerAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.PrognoseAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO.ArbeidsrelatertArsakDTO.ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO.ShortNameDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO.SporsmalOgSvarDTO
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.set
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.OffsetDateTime

fun SykmeldingDTO.tilSykmeldingArbeidsgiver(person: Person): Sykmelding {
    val sykmelding = sendSykmeldingAivenKafkaMessage.sykmelding
    val event = sendSykmeldingAivenKafkaMessage.event
    return Sykmelding(
        orgnrHovedenhet = event.arbeidsgiver.juridiskOrgnummer?.let { Orgnr(it) },
        mottattidspunkt = sykmelding.mottattTidspunkt.toLocalDateTime(),
        sykmeldingId = sykmelding.id,
        orgnr = event.arbeidsgiver.orgnummer.let { Orgnr(it) },
        egenmeldingsdager = event.sporsmals.tilEgenmeldingsdager(),
        arbeidsgiver = sykmelding.arbeidsgiver.tilArbeidsgiver(),
        behandlerNavn = sykmelding.behandler?.tilNavn(),
        behandlerTlf = sykmelding.behandler?.tlf.tolkTelefonNr(),
        kontaktMedPasient = sykmelding.behandletTidspunkt.tilKontaktMedPasient(),
        meldingTilArbeidsgiver = sykmelding.meldingTilArbeidsgiver,
        sykmeldtFnr = Fnr(person.fnr),
        sykmeldtNavn = person.tilNavn(),
        perioder = sykmelding.sykmeldingsperioder.tilPerioderAG(),
        prognose = sykmelding.prognose?.getPrognose(),
        syketilfelleFom = sykmelding.syketilfelleStartDato,
        tiltak = sykmelding.tiltakArbeidsplassen?.tilTiltak(),
    )
}

private fun List<SporsmalOgSvarDTO>?.tilEgenmeldingsdager(): Set<LocalDate> =
    this
        ?.find { it.shortName == ShortNameDTO.EGENMELDINGSDAGER }
        ?.svar
        ?.fromJson(LocalDateSerializer.set())
        ?: emptySet()

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

private fun AktivitetIkkeMuligAGDTO.tilAktivitetIkkeMulig(): AktivitetIkkeMulig =
    AktivitetIkkeMulig(
        manglendeTilretteleggingPaaArbeidsplassen = erMangledneTilrettelegging(this),
        beskrivelse = this.arbeidsrelatertArsak?.beskrivelse,
    )

private fun erMangledneTilrettelegging(aktivitetIkkeMulig: AktivitetIkkeMuligAGDTO): Boolean? =
    aktivitetIkkeMulig.arbeidsrelatertArsak?.arsak?.any { it == MANGLENDE_TILRETTELEGGING }

private fun Person.tilNavn(): Navn =
    Navn(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
    )

private fun OffsetDateTime.tilKontaktMedPasient(): KontaktMedPasient = KontaktMedPasient(behandlet = this.toLocalDateTime())

fun String?.tolkTelefonNr(): String =
    this
        ?.lowercase()
        ?.trim()
        ?.removePrefix("tel:")
        ?.removePrefix("fax:")
        ?: ""

private fun BehandlerAGDTO.tilNavn(): Navn =
    Navn(
        fornavn = this.fornavn,
        etternavn = this.etternavn,
        mellomnavn = this.mellomnavn,
    )

private fun ArbeidsgiverAGDTO.tilArbeidsgiver(): Arbeidsgiver =
    Arbeidsgiver(
        navn = this.navn,
    )
