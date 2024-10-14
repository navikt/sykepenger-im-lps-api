package no.nav.helsearbeidsgiver.kafka

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

suspend fun startKafkaConsumer(
    topic: String,
    lpsKafkaConsumer: LpsKafkaConsumer,
) {
    val consumer = KafkaConsumer<String, String>(createKafkaConsumerConfig() as Map<String, Any>)
    consumer.subscribe(listOf(topic))

    consumer.asFlow().collect { record ->
        lpsKafkaConsumer.handleRecord(record)
        consumer.commitSync()
    }
}

fun <K, V> KafkaConsumer<K, V>.asFlow(timeout: Duration = Duration.ofMillis(10)): Flow<ConsumerRecord<K, V>> =
    flow {
        while (true) {
            poll(timeout).forEach { emit(it) }
        }
    }

interface LpsKafkaConsumer {
    fun handleRecord(record: ConsumerRecord<String, String>)
}
