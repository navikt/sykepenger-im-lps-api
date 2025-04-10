package no.nav.helsearbeidsgiver.kafka.innsending

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.Env.getProperty
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.json.toPretty
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

interface InnsendingProducerI {
    fun send(vararg message: Pair<InnsendingKafka.Key, JsonElement>): Result<JsonElement>
}

class IngenInnsendingProducer : InnsendingProducerI {
    private val topic = getProperty("kafkaProducer.innsending.topic")

    override fun send(vararg message: Pair<InnsendingKafka.Key, JsonElement>): Result<JsonElement> =
        Result
            .success("".toJson())
            .also { sikkerLogger().info("Publiserer ingen melding om innsendt skjema til $topic.") }
}

class InnsendingProducer(
    private val kafkaProducer: KafkaProducer<String, JsonElement>,
) : InnsendingProducerI {
    private val topic = getProperty("kafkaProducer.innsending.topic")

    override fun send(vararg message: Pair<InnsendingKafka.Key, JsonElement>): Result<JsonElement> =
        message
            .toMap()
            .toJson()
            .let(::send)
            .onSuccess {
                sikkerLogger().info("Publiserte melding om innsendt skjema på topic $topic:\n${it.toPretty()}")
            }.onFailure {
                throw it.also {
                    sikkerLogger().error(
                        "Klarte ikke publisere melding om innsendt skjema på topic $topic:\n${
                            message
                                .toMap()
                                .toJson().toPretty()
                        }",
                    )
                }
            }

    private fun Map<InnsendingKafka.Key, JsonElement>.toJson(): JsonElement =
        toJson(
            MapSerializer(
                InnsendingKafka.Key.serializer(),
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
