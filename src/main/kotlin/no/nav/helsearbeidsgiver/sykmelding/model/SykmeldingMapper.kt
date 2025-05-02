@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding.model

import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykmeldt
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka
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

fun SykmeldingDTO.tilSykmelding(): Sykmelding {
    val sykmelding = sendSykmeldingAivenKafkaMessage.sykmelding
    val event = sendSykmeldingAivenKafkaMessage.event
    val kafkaMetadata = sendSykmeldingAivenKafkaMessage.kafkaMetadata
    return Sykmelding(
        sykmeldingId = sykmelding.id,
        mottattAvNav = sykmelding.mottattTidspunkt.toLocalDateTime(),
        arbeidsgiver = sendSykmeldingAivenKafkaMessage.tilArbeidsgiver(),
        egenmeldingsdager = event.sporsmals.tilEgenmeldingsdager(),
        behandler = sykmelding.behandler?.tilBehandler(),
        kontaktMedPasient = sykmelding.behandletTidspunkt.toLocalDateTime(),
        sykmeldt = tilSykmeldt(),
        sykmeldingPerioder = sykmelding.sykmeldingsperioder.tilPerioderAG(),
        sykefravaerFom = sykmelding.syketilfelleStartDato,
        oppfoelging = sykmelding.tilOppfoelging(),
    )
}

private fun SykmeldingDTO.tilSykmeldt(): Sykmeldt =
    Sykmeldt(
        fnr = sendSykmeldingAivenKafkaMessage.kafkaMetadata.fnr.let(::Fnr),
        navn = sykmeldtNavn,
    )

private fun BehandlerAGDTO.tilBehandler(): Behandler =
    Behandler(
        navn = tilFulltNavn(),
        tlf = tlf.tolkTelefonNr(),
    )

private fun ArbeidsgiverSykmeldingKafka.tilOppfoelging(): Oppfoelging =
    Oppfoelging(
        prognose = prognose?.getPrognose(),
        meldingTilArbeidsgiver = meldingTilArbeidsgiver,
        tiltakArbeidsplassen = tiltakArbeidsplassen,
    )

private fun List<SporsmalOgSvarDTO>?.tilEgenmeldingsdager(): List<Periode> =
    this
        ?.find { it.shortName == ShortNameDTO.EGENMELDINGSDAGER }
        ?.svar
        ?.fromJson(LocalDateSerializer.set())
        ?.tilPerioder()
        ?: emptyList()

private fun PrognoseAGDTO.getPrognose(): Prognose =
    Prognose(
        erArbeidsfoerEtterEndtPeriode = arbeidsforEtterPeriode,
        beskrivHensynArbeidsplassen = hensynArbeidsplassen,
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

fun String?.tolkTelefonNr(): String =
    this
        ?.lowercase()
        ?.trim()
        ?.removePrefix("tel:")
        ?.removePrefix("fax:")
        ?: ""

private fun BehandlerAGDTO?.tilFulltNavn(): String = if (this == null) "" else "$fornavn ${mellomnavn.orEmpty()} $etternavn"

private fun SendSykmeldingAivenKafkaMessage.tilArbeidsgiver(): Arbeidsgiver =
    Arbeidsgiver(
        navn = sykmelding.arbeidsgiver.navn,
        orgnr = event.arbeidsgiver.orgnummer.let(::Orgnr),
    )
