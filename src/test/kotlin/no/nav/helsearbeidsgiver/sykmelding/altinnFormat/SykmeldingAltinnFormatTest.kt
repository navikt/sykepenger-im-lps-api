package no.nav.helsearbeidsgiver.sykmelding.altinnFormat

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.sykmelding.model.Person
import no.nav.helsearbeidsgiver.sykmelding.model.tilJson
import no.nav.helsearbeidsgiver.sykmelding.model.toAltinnSykmeldingArbeidsgiver
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.json.JSONObject
import org.json.XML
import org.junit.jupiter.api.Test

class SykmeldingAltinnFormatTest {
    @Test
    fun `tilSykmeldingArbeidsgiver json er identisk til gamle XML formatet`() {
        val periode = sykmeldingMock().sykmelding.sykmeldingsperioder.first()

        val sykmeldingKafkaMessage =
            sykmeldingMock().copy(
                sykmelding =
                    sykmeldingMock().sykmelding.copy(
                        sykmeldingsperioder =
                            listOf(
                                periode,
                                periode.copy(
                                    fom = periode.fom.plusDays(3),
                                    tom = periode.tom.plusDays(3),
                                    innspillTilArbeidsgiver = "Her kan mye gjøres",
                                ),
                            ),
                    ),
            )

        val person =
            Person(
                fornavn = "Ola",
                mellomnavn = null,
                etternavn = "Nordmann",
                aktorId = "aktorId",
                fnr = sykmeldingKafkaMessage.kafkaMetadata.fnr,
            )

        val xmlSykmeldingArbeidsgiver =
            SykmeldingArbeidsgiverMapper.toAltinnXMLSykmelding(
                sendtSykmeldingKafkaMessage = sykmeldingKafkaMessage,
                person = person,
                egenmeldingsdager = null,
            )

        val xmlString = JAXB.marshallSykmeldingArbeidsgiver(xmlSykmeldingArbeidsgiver)
        val expectedJson = XML.toJSONObject(xmlString)

        val sykmeldingArbeidsgiver =
            toAltinnSykmeldingArbeidsgiver(
                sendtSykmeldingKafkaMessage = sykmeldingKafkaMessage,
                person = person,
                egenmeldingsdager = null,
            )

        val actualJson = sykmeldingArbeidsgiver.tilJson()

//        val expectedJson = JSONObject("""{value: "expected value", v2:"b"}  """)

        JSONObject(actualJson).toString() shouldBe expectedJson.toString()
    }
}
