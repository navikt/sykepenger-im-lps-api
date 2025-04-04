package no.nav.helsearbeidsgiver.sykmelding.model

import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.ArbeidsgiverAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.BehandlerAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.PrognoseAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingResponse
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional.ofNullable
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

fun tilSykmeldingArbeidsgiver(
    sykmelding: SykmeldingResponse,
    person: Person,
): SykmeldingArbeidsgiver =
    SykmeldingArbeidsgiver(
        juridiskOrganisasjonsnummer = 0,
        mottattidspunkt = sykmelding.arbeidsgiverSykmeldingKafka.mottattTidspunkt.toLocalDateTime(),
        sykmeldingId = sykmelding.id,
        virksomhetsnummer = sykmelding.orgnr.toLong(),
        sykmelding = toXMLSykmelding(sykmelding.arbeidsgiverSykmeldingKafka, person, null), // TODO: Egenmeldingsdager
        xmlns = "http://nav.no/melding/virksomhet/sykmeldingArbeidsgiver/v1/sykmeldingArbeidsgiver",
    )

private fun toXMLSykmelding(
    sykmelding: ArbeidsgiverSykmeldingKafka,
    person: Person,
    egenmeldingsdager: List<LocalDate>?,
): Sykmelding {
    val sendtSykmelding = sykmelding

    return Sykmelding(
        arbeidsgiver = getArbeidsgiver(sendtSykmelding.arbeidsgiver),
        behandler = getBehandler(sendtSykmelding.behandler),
        kontaktMedPasient = getKontaktMedPasient(sendtSykmelding.behandletTidspunkt),
        meldingTilArbeidsgiver = getMeldingTilArbeidsgiver(sendtSykmelding.meldingTilArbeidsgiver),
        pasient = getPasient(person.fnr, person),
        perioder = getPerioderAG(sendtSykmelding.sykmeldingsperioder),
        prognose = getPrognose(sendtSykmelding.prognose),
        syketilfelleFom = sendtSykmelding.syketilfelleStartDato,
        tiltak = getTiltak(sendtSykmelding.tiltakArbeidsplassen),
        egenmeldingsdager = getEgenmeldingsdager(egenmeldingsdager),
    )
}

private fun getEgenmeldingsdager(egenmeldingsdager: List<LocalDate>?): Egenmeldingsdager? =
    if (egenmeldingsdager.isNullOrEmpty()) {
        null
    } else {
        Egenmeldingsdager(
            dager = egenmeldingsdager,
        )
    }

private fun getTiltak(tiltakArbeidsplassen: String?): Tiltak? =
    when (tiltakArbeidsplassen) {
        null -> null
        else -> {
            Tiltak(tiltakArbeidsplassen = tiltakArbeidsplassen)
        }
    }

private fun getPrognose(prognose: PrognoseAGDTO?): Prognose? =
    when (prognose) {
        null -> null
        else -> {
            Prognose(
                erArbeidsfoerEtterEndtPeriode = prognose.arbeidsforEtterPeriode,
                beskrivHensynArbeidsplassen = prognose.hensynArbeidsplassen,
            )
        }
    }

private fun getPerioderAG(sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>): List<Periode> =
    sykmeldingsperioder.map {
        Periode(
            fom = it.fom,
            tom = it.tom,
            aktivitet = getAktivitet(it),
        )
    }

private fun getAktivitet(it: SykmeldingsperiodeAGDTO): Aktivitet {
    val harReisetilskudd = if (it.reisetilskudd) true else null

    return Aktivitet(
        avventendeSykmelding = it.innspillTilArbeidsgiver,
        gradertSykmelding = it.gradert?.let { GradertSykmelding(it.grad, it.reisetilskudd) },
        aktivitetIkkeMulig = getAktivitetIkkeMulig(it.aktivitetIkkeMulig),
        harReisetilskudd = harReisetilskudd,
        antallBehandlingsdagerUke = it.behandlingsdager,
    )
}

private fun getAktivitetIkkeMulig(aktivitetIkkeMulig: SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO?): AktivitetIkkeMulig? =
    when (aktivitetIkkeMulig) {
        null -> null
        else -> {
            AktivitetIkkeMulig(
                manglendeTilretteleggingPaaArbeidsplassen =
                    isMangledneTilrettelegging(aktivitetIkkeMulig),
                beskrivelse = aktivitetIkkeMulig.arbeidsrelatertArsak?.beskrivelse,
            )
        }
    }

private fun isMangledneTilrettelegging(aktivitetIkkeMulig: SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO): Boolean? =
    aktivitetIkkeMulig.arbeidsrelatertArsak?.arsak?.any {
        it ==
            SykmeldingsperiodeAGDTO
                .AktivitetIkkeMuligAGDTO
                .ArbeidsrelatertArsakDTO
                .ArbeidsrelatertArsakTypeDTO
                .MANGLENDE_TILRETTELEGGING
    }

private fun getPasient(
    fnr: String,
    person: Person,
): Pasient {
    val navn =
        Navn(
            fornavn = person.fornavn,
            mellomnavn = person.mellomnavn,
            etternavn = person.etternavn,
        )

    return Pasient(
        navn = navn,
        ident = fnr,
    )
}

private fun getMeldingTilArbeidsgiver(meldingTilArbeidsgiver: String?): String? = meldingTilArbeidsgiver

private fun getKontaktMedPasient(kontaktMedPasient: OffsetDateTime): KontaktMedPasient =
    KontaktMedPasient(behandlet = kontaktMedPasient.toLocalDateTime())

private fun getBehandler(behandler: BehandlerAGDTO?): Behandler =
    Behandler(
        navn = getNavn(behandler),
        telefonnummer = getTelefonnr(behandler?.tlf).toLong(),
    )

private fun getTelefonnr(telefonnr: String?): String = ofNullable(telefonnr).map(removePrefix).orElseGet { telefonnr } ?: ""

private val removePrefix =
    Function<String, String?> { kontaktinfo: String? ->
        ofNullable(kontaktinfo)
            .map { s: String? ->
                Pattern
                    .compile(
                        "(tel|fax):(\\d+)",
                        Pattern.CASE_INSENSITIVE,
                    ).matcher(s)
            }.filter { obj: Matcher -> obj.matches() }
            .filter { matcher: Matcher -> matcher.groupCount() == 2 }
            .map { matcher: Matcher ->
                matcher.group(
                    2,
                )
            }.map { obj: String -> obj.trim { it <= ' ' } }
            .orElse(kontaktinfo)
    }

private fun getNavn(behandler: BehandlerAGDTO?): Navn =
    Navn(
        fornavn = behandler?.fornavn ?: "",
        etternavn = behandler?.etternavn ?: "",
        mellomnavn = behandler?.mellomnavn ?: "",
    )

private fun getArbeidsgiver(arbeidsgiver: ArbeidsgiverAGDTO): Arbeidsgiver? =
    Arbeidsgiver(
        navn = arbeidsgiver.navn,
    )
