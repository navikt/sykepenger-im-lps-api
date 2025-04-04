package no.nav.helsearbeidsgiver.sykmelding.altinnFormat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.sykmelding.model.SykmeldingArbeidsgiver

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
