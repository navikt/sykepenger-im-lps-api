@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding.model

import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.BehandlerAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.PrognoseAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO.ArbeidsrelatertArsakDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO.ArbeidsrelatertArsakDTO.ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.GradertDTO
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO.ShortNameDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO.SporsmalOgSvarDTO
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.set
import no.nav.helsearbeidsgiver.utils.tilPerioder
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.OffsetDateTime

fun SykmeldingDTO.tilSykmelding(person: Person): Sykmelding {
    val sykmelding = sendSykmeldingAivenKafkaMessage.sykmelding
    val event = sendSykmeldingAivenKafkaMessage.event
    return Sykmelding(
        sykmeldingId = sykmelding.id,
        mottattidspunkt = sykmelding.mottattTidspunkt.toLocalDateTime(),
        arbeidsgiver = sendSykmeldingAivenKafkaMessage.tilArbeidsgiver(),
        egenmeldingsdager = event.sporsmals.tilEgenmeldingsdager(),
        behandlerNavn = sykmelding.behandler.tilNavn(),
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

private fun List<SporsmalOgSvarDTO>?.tilEgenmeldingsdager(): Set<Periode> =
    this
        ?.find { it.shortName == ShortNameDTO.EGENMELDINGSDAGER }
        ?.svar
        ?.fromJson(LocalDateSerializer.set())
        ?.tilPerioder()
        ?: emptySet()

private fun String.tilTiltak(): Tiltak = Tiltak(tiltakArbeidsplassen = this)

private fun PrognoseAGDTO.getPrognose(): Prognose =
    Prognose(
        erArbeidsfoerEtterEndtPeriode = this.arbeidsforEtterPeriode,
        beskrivHensynArbeidsplassen = this.hensynArbeidsplassen,
    )

private fun List<SykmeldingsperiodeAGDTO>.tilPerioderAG(): List<SykmeldingPeriode> =
    map {
        SykmeldingPeriode(
            fom = it.fom,
            tom = it.tom,
            aktivitet = it.tilAktivitet(),
        )
    }

private fun SykmeldingsperiodeAGDTO.tilAktivitet(): Aktivitet =
    Aktivitet(
        avventendeSykmelding = innspillTilArbeidsgiver,
        gradertSykmelding = gradert?.tilGradertSykmelding(),
        aktivitetIkkeMulig = aktivitetIkkeMulig?.arbeidsrelatertArsak?.tilAktivitetIkkeMulig(),
        harReisetilskudd = reisetilskudd,
        antallBehandlingsdagerUke = behandlingsdager,
    )

private fun ArbeidsrelatertArsakDTO.tilAktivitetIkkeMulig(): AktivitetIkkeMulig =
    AktivitetIkkeMulig(
        manglendeTilretteleggingPaaArbeidsplassen = arsak.any { it == MANGLENDE_TILRETTELEGGING },
        beskrivelse = beskrivelse,
    )

private fun GradertDTO.tilGradertSykmelding(): GradertSykmelding = GradertSykmelding(grad, reisetilskudd)

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

private fun BehandlerAGDTO?.tilNavn(): Navn =
    Navn(
        fornavn = this?.fornavn ?: "",
        etternavn = this?.etternavn ?: "",
        mellomnavn = this?.mellomnavn ?: "",
    )

private fun SendSykmeldingAivenKafkaMessage.tilArbeidsgiver(): Arbeidsgiver =
    Arbeidsgiver(
        navnFraBehandler = sykmelding.arbeidsgiver.navn,
        orgnrHovedenhet = event.arbeidsgiver.juridiskOrgnummer?.let { Orgnr(it) },
        orgnr = event.arbeidsgiver.orgnummer.let { Orgnr(it) },
    )
