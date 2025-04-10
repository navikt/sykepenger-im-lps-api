package no.nav.helsearbeidsgiver.kafka.forespoersel.pri

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BehovMessage(
    @SerialName("@behov") val behov: String,
)
