@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding.model

import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.ArbeidsgiverAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.BehandlerAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.PrognoseAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import java.time.LocalDate
import java.time.OffsetDateTime

fun SykmeldingDTO.tilSykmeldingArbeidsgiver(person: Person): SykmeldingArbeidsgiver {
    val sykmelding = sendSykmeldingAivenKafkaMessage.sykmelding
    val event = sendSykmeldingAivenKafkaMessage.event
    return SykmeldingArbeidsgiver(
        orgnrHovedenhet = event.arbeidsgiver.juridiskOrgnummer,
        mottattidspunkt = sykmelding.mottattTidspunkt.toLocalDateTime(),
        sykmeldingId = sykmelding.id,
        orgnr = event.arbeidsgiver.orgnummer,
        egenmeldingsdager = event.sporsmals.tilEgenmeldingsdager(),
        arbeidsgiver = sykmelding.arbeidsgiver.tilArbeidsgiver(),
        behandler = sykmelding.behandler.tilBehandler(),
        kontaktMedPasient = sykmelding.behandletTidspunkt.tilKontaktMedPasient(),
        meldingTilArbeidsgiver = sykmelding.meldingTilArbeidsgiver,
        sykmeldtFnr = person.fnr,
        sykmeldtNavn = person.tilNavn(),
        perioder = sykmelding.sykmeldingsperioder.tilPerioderAG(),
        prognose = sykmelding.prognose?.getPrognose(),
        syketilfelleFom = sykmelding.syketilfelleStartDato,
        tiltak = sykmelding.tiltakArbeidsplassen?.tilTiltak(),
    )
}

private fun List<SykmeldingStatusKafkaEventDTO.SporsmalOgSvarDTO>?.tilEgenmeldingsdager(): Egenmeldingsdager? =
    this
        ?.find { it.shortName == SykmeldingStatusKafkaEventDTO.ShortNameDTO.EGENMELDINGSDAGER }
        ?.let { Json.decodeFromString<List<String>>(it.svar) }
        ?.map { LocalDate.parse(it) }
        ?.let { Egenmeldingsdager(it) }

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

private fun Person.tilNavn(): Navn =
    Navn(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
    )

private fun OffsetDateTime.tilKontaktMedPasient(): KontaktMedPasient = KontaktMedPasient(behandlet = this.toLocalDateTime())

private fun BehandlerAGDTO?.tilBehandler(): Behandler =
    Behandler(
        navn = tilNavn(),
        telefonnummer = this?.tlf.tolkTelefonNr(),
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
