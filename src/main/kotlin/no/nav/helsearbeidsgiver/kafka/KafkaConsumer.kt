package no.nav.helsearbeidsgiver.kafka

import no.nav.helsearbeidsgiver.Env
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

fun startKafkaConsumer() {
    val consumer = KafkaConsumer<String, String>(createKafkaConsumerConfig() as Map<String, Any>)
    val topic = Env.getProperty("kafkaConsumer.topic")
    consumer.subscribe(listOf(topic))
    val meldingService = KafkaInnkommendeMeldingService()
    var running = true
    while (running) {
        val records = consumer.poll(Duration.ofMillis(10))
        for (record in records) {
            meldingService.behandle(record.value())
            consumer.commitSync()
        }
    }
}
