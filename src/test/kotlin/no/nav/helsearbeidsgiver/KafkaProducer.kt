package no.nav.helsearbeidsgiver

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

fun <K, V> KafkaProducer<K, V>.sendAsync(record: ProducerRecord<K, V>): Deferred<RecordMetadata> =
    CompletableDeferred<RecordMetadata>().apply {
        send(record) { metadata, exception ->
            if (exception != null) {
                completeExceptionally(exception)
            } else {
                complete(metadata)
            }
        }
    }
