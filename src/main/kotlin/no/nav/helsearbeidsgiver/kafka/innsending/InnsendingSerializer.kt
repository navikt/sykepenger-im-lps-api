package no.nav.helsearbeidsgiver.kafka.innsending

import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.utils.json.toJson

class InnsendingSerializer : org.apache.kafka.common.serialization.Serializer<JsonElement> {
    override fun serialize(
        topic: String,
        data: JsonElement,
    ): ByteArray =
        data
            .toJson(JsonElement.serializer())
            .toString()
            .toByteArray()
}
