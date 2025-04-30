package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.Env.getProperty
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.json.toPretty
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class DialogProducer(
    private val kafkaProducer: KafkaProducer<String, JsonElement>,
) {
    private val topic = getProperty("kafkaProducer.dialog.topic")

    fun send(dialogMelding: DialogMelding) {
        val message = dialogMelding.toJson(DialogMelding.serializer())
        runCatching {
            kafkaProducer.send(message.toRecord()).get()
        }.map { message }
            .onSuccess {
                sikkerLogger().info("Publiserte melding om dialog skjema på topic $topic:\n${it.toPretty()}")
            }.getOrElse {
                sikkerLogger().error("Klarte ikke publisere melding om innsendt skjema på topic $topic:\n${message.toPretty()}")
                throw it
            }
    }

    private fun JsonElement.toRecord(): ProducerRecord<String, JsonElement> = ProducerRecord(topic, this)
}
