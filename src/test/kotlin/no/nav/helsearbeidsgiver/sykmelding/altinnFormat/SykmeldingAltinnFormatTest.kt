package no.nav.helsearbeidsgiver.sykmelding.altinnFormat

import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.model.Person
import no.nav.helsearbeidsgiver.sykmelding.model.SykmeldingArbeidsgiver
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmeldingArbeidsgiver
import no.nav.helsearbeidsgiver.sykmelding.toSykmeldingResponse
import no.nav.helsearbeidsgiver.utils.TestData.XML_ARBEIDSGIVERPERIODE
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.json.JSONObject
import org.junit.jupiter.api.Test

class SykmeldingAltinnFormatTest {
    @Test
    fun `tilAltinnSykmeldingArbeidsgiver json schema er identisk til tidligere XML format`() {
        val sykmeldingKafkaMessage = sykmeldingMock().dupliserPeriode()
        val person = mockPerson(sykmeldingKafkaMessage.kafkaMetadata.fnr)

        // ny implementasjon med @Serializable data class
        val sykmeldingArbeidsgiver = tilSykmeldingArbeidsgiver(sykmeldingKafkaMessage.toSykmeldingResponse(), person)
        val jsonString = sykmeldingArbeidsgiver.tilJson()

        JSONObject(jsonString).toString() shouldBe XML_ARBEIDSGIVERPERIODE
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

val json =
    Json {
        encodeDefaults = false
        explicitNulls = false // ikke inkluder null verdier
    }

fun SykmeldingArbeidsgiver.tilJson(): String {
    val wrapper = SykmeldingArbeidsgiverWrapper(this)
    return json.encodeToString(wrapper)
}

@Serializable
data class SykmeldingArbeidsgiverWrapper(
    @SerialName("ns2:sykmeldingArbeidsgiver")
    val sykmeldingArbeidsgiver: SykmeldingArbeidsgiver,
)
