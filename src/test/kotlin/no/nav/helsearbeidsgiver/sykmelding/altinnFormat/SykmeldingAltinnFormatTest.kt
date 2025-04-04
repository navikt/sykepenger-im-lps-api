package no.nav.helsearbeidsgiver.sykmelding.altinnFormat

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.model.Person
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmeldingArbeidsgiver
import no.nav.helsearbeidsgiver.sykmelding.toSykmeldingResponse
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.json.JSONObject
import org.json.XML
import org.junit.jupiter.api.Test

class SykmeldingAltinnFormatTest {
    @Test
    fun `tilAltinnSykmeldingArbeidsgiver json schema er identisk til tidligere XML format`() {
        val sykmeldingKafkaMessage = sykmeldingMock().dupliserPeriode()
        val person = mockPerson(sykmeldingKafkaMessage.kafkaMetadata.fnr)

        // Gammel versjon fra Syfosmaltinn
        val xmlMapper = SykmeldingArbeidsgiverMapper
        val xmlSykmeldingArbeidsgiver = xmlMapper.toAltinnXMLSykmelding(sykmeldingKafkaMessage, person, null)
        val xmlString = JAXB.marshallSykmeldingArbeidsgiver(xmlSykmeldingArbeidsgiver)

        // ny implementasjon med @Serializable data class
        val sykmeldingArbeidsgiver = tilSykmeldingArbeidsgiver(sykmeldingKafkaMessage.toSykmeldingResponse(), person)
        val jsonString = sykmeldingArbeidsgiver.tilJson()

        JSONObject(jsonString).toString() shouldBe XML.toJSONObject(xmlString).toString()
    }
}

fun SendSykmeldingAivenKafkaMessage.dupliserPeriode(): SendSykmeldingAivenKafkaMessage =
    copy(sykmelding = sykmelding.copy(sykmeldingsperioder = List(2) { sykmelding.sykmeldingsperioder.first() }))

fun mockPerson(fnr: String): Person =
    Person(
        fornavn = "Ola",
        mellomnavn = null,
        etternavn = "Nordmann",
        aktorId = "aktorId",
        fnr = fnr,
    )
