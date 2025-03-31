package no.nav.helsearbeidsgiver.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.sykmelding.model.SykmeldingArbeidsgiver
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val json =
    Json {
        encodeDefaults = false
        explicitNulls = false // ikke inkluder null verdier
    }

@Serializable
data class SykmeldingArbeidsgiverWrapper(
    @SerialName("ns2:sykmeldingArbeidsgiver")
    val sykmeldingArbeidsgiver: SykmeldingArbeidsgiver,
)

@Serializer(forClass = LocalDateTime::class)
object XMLLocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(
        encoder: Encoder,
        value: LocalDateTime,
    ) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString(), formatter)
}
