package no.nav.helsearbeidsgiver.dokumentkobling

import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.Env.getProperty
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.json.toPretty
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class DokumentkoblingProducer(
    private val kafkaProducer: KafkaProducer<String, JsonElement>,
) {
    private val topic = getProperty("kafkaProducer.dokument_kobling.topic")

    fun send(dokumentkobling: Dokumentkobling) {
        val message = dokumentkobling.toJson(Dokumentkobling.serializer())
        runCatching {
            kafkaProducer.send(message.toRecord()).get()
        }.map { message }
            .onSuccess {
                sikkerLogger().info("Publiserte melding om dokumentkobling på topic $topic:\n${it.toPretty()}")
            }.getOrElse {
                sikkerLogger().error("Klarte ikke publisere melding om dokumentkobling på topic $topic:\n${message.toPretty()}")
                throw it
            }
    }

    private fun JsonElement.toRecord(): ProducerRecord<String, JsonElement> = ProducerRecord(topic, this)
}
