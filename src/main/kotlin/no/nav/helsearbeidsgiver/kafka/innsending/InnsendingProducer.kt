package no.nav.helsearbeidsgiver.kafka.innsending

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.Env.getProperty
import no.nav.helsearbeidsgiver.innsending.Innsending
import no.nav.helsearbeidsgiver.utils.json.toJson
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class InnsendingProducer(
    private val kafkaProducer: KafkaProducer<String, JsonElement>,
) {
    private val topic = getProperty("kafkaProducer.innsending.topic")

    fun send(vararg message: Pair<Innsending.Key, JsonElement>): Result<JsonElement> =
        message
            .toMap()
            .toJson()
            .let(::send)

    private fun Map<Innsending.Key, JsonElement>.toJson(): JsonElement =
        toJson(
            MapSerializer(
                Innsending.Key.serializer(),
                JsonElement.serializer(),
            ),
        )

    private fun send(message: JsonElement): Result<JsonElement> =
        message
            .toRecord()
            .runCatching {
                kafkaProducer.send(this).get()
            }.map { message }

    private fun JsonElement.toRecord(): ProducerRecord<String, JsonElement> = ProducerRecord(topic, this)
}
