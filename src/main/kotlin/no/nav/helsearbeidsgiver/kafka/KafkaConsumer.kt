package no.nav.helsearbeidsgiver.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

fun startKafkaConsumer(
    topic: String,
    lpsKafkaConsumer: LpsKafkaConsumer,
) {
    val consumer = KafkaConsumer<String, String>(createKafkaConsumerConfig() as Map<String, Any>)
    consumer.subscribe(listOf(topic))
    var running = true
    while (running) {
        val records = consumer.poll(Duration.ofMillis(10))
        for (record in records) {
            lpsKafkaConsumer.handleRecord(record)
            consumer.commitSync()
        }
    }
}

// fun <K, V> KafkaConsumer<K, V>.asFlow(timeout: Duration = Duration.ofMillis(10)): Flow<ConsumerRecord<K, V>> =
//    flow {
//        while (true) {
//            poll(timeout).forEach { emit(it) }
//        }
//    }

interface LpsKafkaConsumer {
    fun handleRecord(record: ConsumerRecord<String, String>)
}
