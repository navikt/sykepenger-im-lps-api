@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.helsearbeidsgiver.sykmelding.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmelding.*
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingOrgnrManglerException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional.ofNullable
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

val json =
    Json {
        encodeDefaults = false
        explicitNulls = false // ikke inkluder null verdier
    }

@Serializable
private data class SykmeldingArbeidsgiverWrapper(
    val sykmeldingArbeidsgiver: SykmeldingArbeidsgiver,
)

fun SykmeldingArbeidsgiver.tilJson(): String {
    val wrapper = SykmeldingArbeidsgiverWrapper(this)
    return json.encodeToString(wrapper)
}

fun toAltinnSykmeldingArbeidsgiver(
    sendtSykmeldingKafkaMessage: SendSykmeldingAivenKafkaMessage,
    person: Person,
    egenmeldingsdager: List<LocalDate>?,
): SykmeldingArbeidsgiver {
    if (sendtSykmeldingKafkaMessage.event.arbeidsgiver?.juridiskOrgnummer == null) {
        throw SykmeldingOrgnrManglerException(
            "Juridisk orgnummer mangler i sykmelding ${sendtSykmeldingKafkaMessage.sykmelding.id}",
        )
    }

    return SykmeldingArbeidsgiver(
        juridiskOrganisasjonsnummer =
            sendtSykmeldingKafkaMessage.event.arbeidsgiver.juridiskOrgnummer
                .toLong(),
        mottattidspunkt = sendtSykmeldingKafkaMessage.sykmelding.mottattTidspunkt.toLocalDateTime(),
        sykmeldingId = sendtSykmeldingKafkaMessage.sykmelding.id,
        virksomhetsnummer =
            sendtSykmeldingKafkaMessage.event.arbeidsgiver.orgnummer
                .toLong(),
        sykmelding = toXMLSykmelding(sendtSykmeldingKafkaMessage, person, egenmeldingsdager),
    )
}

private fun toXMLSykmelding(
    sendtSykmeldingKafkaMessage: SendSykmeldingAivenKafkaMessage,
    person: Person,
    egenmeldingsdager: List<LocalDate>?,
): Sykmelding {
    val sendtSykmelding = sendtSykmeldingKafkaMessage.sykmelding

    return Sykmelding(
        arbeidsgiver = getArbeidsgiver(sendtSykmelding.arbeidsgiver),
        behandler = getBehandler(sendtSykmelding.behandler),
        kontaktMedPasient = getKontaktMedPasient(sendtSykmelding.behandletTidspunkt),
        meldingTilArbeidsgiver = getMeldingTilArbeidsgiver(sendtSykmelding.meldingTilArbeidsgiver),
        pasient = getPasient(sendtSykmeldingKafkaMessage.kafkaMetadata, person),
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
    val harReisetilskudd =
        it.reisetilskudd.let {
            when (it) {
                true -> true
                else -> null
            }
        }

    return Aktivitet(
        avventendeSykmelding = it.innspillTilArbeidsgiver,
        gradertSykmelding = getGradertAktivitet(it.gradert),
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

private fun isMangledneTilrettelegging(aktivitetIkkeMulig: SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO?): Boolean? =
    aktivitetIkkeMulig!!.arbeidsrelatertArsak?.arsak?.stream()?.anyMatch {
        it ==
            SykmeldingsperiodeAGDTO
                .AktivitetIkkeMuligAGDTO
                .ArbeidsrelatertArsakDTO
                .ArbeidsrelatertArsakTypeDTO
                .MANGLENDE_TILRETTELEGGING
    }

private fun getGradertAktivitet(gradert: SykmeldingsperiodeAGDTO.GradertDTO?): GradertSykmelding? =
    when (gradert) {
        null -> null
        else -> {
            GradertSykmelding(
                sykmeldingsgrad = gradert.grad,
                harReisetilskudd = gradert.reisetilskudd,
            )
        }
    }

private fun getPasient(
    metadata: no.nav.helsearbeidsgiver.sykmelding.KafkaMetadataDTO,
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
        ident = metadata.fnr,
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
