@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.helsearbeidsgiver.sykmelding.altinnFormat

import no.nav.helse.xml.sykmelding.arbeidsgiver.ObjectFactory
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLAktivitet
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLAktivitetIkkeMulig
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLArbeidsgiver
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLBehandler
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLEgenmeldingsdager
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLGradertSykmelding
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLKontaktMedPasient
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLNavn
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLPasient
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLPeriode
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLPrognose
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLSykmelding
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLSykmeldingArbeidsgiver
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLTiltak
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmelding.*
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.model.Person
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional.ofNullable
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

class SykmeldingArbeidsgiverMapper private constructor() {
    companion object {
        fun toAltinnXMLSykmelding(
            sendtSykmeldingKafkaMessage: SendSykmeldingAivenKafkaMessage,
            person: Person,
            egenmeldingsdager: List<LocalDate>?,
        ): XMLSykmeldingArbeidsgiver {
            val xmlSykmeldingArbeidsgiver = ObjectFactory().createXMLSykmeldingArbeidsgiver()
            xmlSykmeldingArbeidsgiver.juridiskOrganisasjonsnummer =
                sendtSykmeldingKafkaMessage.event.arbeidsgiver!!.juridiskOrgnummer
            xmlSykmeldingArbeidsgiver.mottattidspunkt =
                sendtSykmeldingKafkaMessage.sykmelding.mottattTidspunkt.toLocalDateTime()
            xmlSykmeldingArbeidsgiver.sykmeldingId = sendtSykmeldingKafkaMessage.sykmelding.id
            xmlSykmeldingArbeidsgiver.virksomhetsnummer =
                sendtSykmeldingKafkaMessage.event.arbeidsgiver!!.orgnummer
            xmlSykmeldingArbeidsgiver.sykmelding =
                toXMLSykmelding(sendtSykmeldingKafkaMessage, person, egenmeldingsdager)
            return xmlSykmeldingArbeidsgiver
        }

        private fun toXMLSykmelding(
            sendtSykmeldingKafkaMessage: SendSykmeldingAivenKafkaMessage,
            person: Person,
            egenmeldingsdager: List<LocalDate>?,
        ): XMLSykmelding {
            val sendtSykmelding = sendtSykmeldingKafkaMessage.sykmelding
            val xmlSykmelding = ObjectFactory().createXMLSykmelding()
            xmlSykmelding.arbeidsgiver = getArbeidsgiver(sendtSykmelding.arbeidsgiver)
            xmlSykmelding.behandler = getBehandler(sendtSykmelding.behandler)
            xmlSykmelding.kontaktMedPasient =
                getKontaktMedPasient(sendtSykmelding.behandletTidspunkt)
            xmlSykmelding.meldingTilArbeidsgiver =
                getMeldingTilArbeidsgiver(sendtSykmelding.meldingTilArbeidsgiver)
            xmlSykmelding.pasient = getPasient(sendtSykmeldingKafkaMessage.kafkaMetadata, person)
            xmlSykmelding.perioder.addAll(getPerioderAG(sendtSykmelding.sykmeldingsperioder))
            xmlSykmelding.prognose = getPrognose(sendtSykmelding.prognose)
            xmlSykmelding.syketilfelleFom = sendtSykmelding.syketilfelleStartDato
            xmlSykmelding.tiltak = getTiltak(sendtSykmelding.tiltakArbeidsplassen)
            xmlSykmelding.egenmeldingsdager = getEgenmeldingsdager(egenmeldingsdager)
            return xmlSykmelding
        }

        private fun getEgenmeldingsdager(egenmeldingsdager: List<LocalDate>?): XMLEgenmeldingsdager? =
            if (egenmeldingsdager.isNullOrEmpty()) {
                null
            } else {
                val xmlEgenmeldingsdager = ObjectFactory().createXMLEgenmeldingsdager()

                egenmeldingsdager.map { xmlEgenmeldingsdager.dager.add(it) }

                xmlEgenmeldingsdager
            }

        private fun getTiltak(tiltakArbeidsplassen: String?): XMLTiltak? =
            when (tiltakArbeidsplassen) {
                null -> null
                else -> {
                    val xmlTiltak = ObjectFactory().createXMLTiltak()
                    xmlTiltak.tiltakArbeidsplassen = tiltakArbeidsplassen
                    xmlTiltak
                }
            }

        private fun getPrognose(prognose: PrognoseAGDTO?): XMLPrognose? =
            when (prognose) {
                null -> null
                else -> {
                    val xmlPrognose = ObjectFactory().createXMLPrognose()
                    xmlPrognose.isErArbeidsfoerEtterEndtPeriode = prognose.arbeidsforEtterPeriode
                    xmlPrognose.beskrivHensynArbeidsplassen = prognose.hensynArbeidsplassen
                    xmlPrognose
                }
            }

        private fun getPerioderAG(sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>): List<XMLPeriode> =
            sykmeldingsperioder.map {
                val periode = XMLPeriode()
                periode.fom = it.fom
                periode.tom = it.tom
                periode.aktivitet = getAktivitet(it)
                periode
            }

        private fun getAktivitet(it: SykmeldingsperiodeAGDTO): XMLAktivitet {
            val xmlAktivitet = ObjectFactory().createXMLAktivitet()
            xmlAktivitet.avventendeSykmelding = it.innspillTilArbeidsgiver
            xmlAktivitet.gradertSykmelding = getGradertAktivitet(it.gradert)
            xmlAktivitet.aktivitetIkkeMulig = getAktivitetIkkeMulig(it.aktivitetIkkeMulig)
            xmlAktivitet.isHarReisetilskudd =
                it.reisetilskudd.let {
                    when (it) {
                        true -> true
                        else -> null
                    }
                }
            xmlAktivitet.antallBehandlingsdagerUke = it.behandlingsdager
            return xmlAktivitet
        }

        private fun getAktivitetIkkeMulig(aktivitetIkkeMulig: SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO?): XMLAktivitetIkkeMulig? =
            when (aktivitetIkkeMulig) {
                null -> null
                else -> {
                    val xmlAktivitetIkkeMulig = ObjectFactory().createXMLAktivitetIkkeMulig()
                    xmlAktivitetIkkeMulig.isManglendeTilretteleggingPaaArbeidsplassen =
                        isMangledneTilrettelegging(aktivitetIkkeMulig)
                    xmlAktivitetIkkeMulig.beskrivelse =
                        aktivitetIkkeMulig.arbeidsrelatertArsak?.beskrivelse
                    xmlAktivitetIkkeMulig
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

        private fun getGradertAktivitet(gradert: SykmeldingsperiodeAGDTO.GradertDTO?): XMLGradertSykmelding? =
            when (gradert) {
                null -> null
                else -> {
                    val xmlGradertSykmelding = ObjectFactory().createXMLGradertSykmelding()
                    xmlGradertSykmelding.sykmeldingsgrad = gradert.grad
                    xmlGradertSykmelding.isHarReisetilskudd = gradert.reisetilskudd
                    xmlGradertSykmelding
                }
            }

        private fun getPasient(
            metadata: no.nav.helsearbeidsgiver.sykmelding.KafkaMetadataDTO,
            person: Person,
        ): XMLPasient? {
            val pasient = ObjectFactory().createXMLPasient()
            pasient.ident = metadata.fnr
            val xmlNavn = XMLNavn()
            xmlNavn.fornavn = person.fornavn
            xmlNavn.mellomnavn = person.mellomnavn
            xmlNavn.etternavn = person.etternavn
            pasient.navn = xmlNavn
            return pasient
        }

        private fun getMeldingTilArbeidsgiver(meldingTilArbeidsgiver: String?): String? = meldingTilArbeidsgiver

        private fun getKontaktMedPasient(kontaktMedPasient: OffsetDateTime): XMLKontaktMedPasient? {
            val xmlKontaktMedPasient = ObjectFactory().createXMLKontaktMedPasient()
            xmlKontaktMedPasient.behandlet = kontaktMedPasient.toLocalDateTime()
            return xmlKontaktMedPasient
        }

        private fun getBehandler(behandler: BehandlerAGDTO?): XMLBehandler? {
            val xmlBehandler = ObjectFactory().createXMLBehandler()
            xmlBehandler.navn = getNavn(behandler)
            xmlBehandler.telefonnummer = getTelefonnr(behandler?.tlf)
            return xmlBehandler
        }

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

        private fun getNavn(behandler: BehandlerAGDTO?): XMLNavn? {
            val xmlNavn = ObjectFactory().createXMLNavn()
            xmlNavn.fornavn = behandler?.fornavn ?: ""
            xmlNavn.etternavn = behandler?.etternavn ?: ""
            xmlNavn.mellomnavn = behandler?.mellomnavn ?: ""
            return xmlNavn
        }

        private fun getArbeidsgiver(arbeidsgiver: ArbeidsgiverAGDTO): XMLArbeidsgiver? {
            val xmlArbeidsgiver = ObjectFactory().createXMLArbeidsgiver()
            xmlArbeidsgiver.navn = arbeidsgiver.navn
            return xmlArbeidsgiver
        }
    }
}
