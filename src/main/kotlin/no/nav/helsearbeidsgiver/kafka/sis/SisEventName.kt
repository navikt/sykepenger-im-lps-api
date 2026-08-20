package no.nav.helsearbeidsgiver.kafka.sis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SisEventName {
    @SerialName("vedtak_fattet")
    VEDTAK_FATTET,

    @SerialName("behandlingstatus")
    BEHANDLINGSTATUS,
}

@Serializable
data class EventNameWrapper(
    val eventName: SisEventName? = null,
)
