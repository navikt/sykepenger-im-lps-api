package helsearbeidsgiver.nav.no.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun startKafkaConsumer() {
    val consumer = KafkaConsumer<String, String>(createKafkaConsumerConfig() as Map<String, Any>)
    consumer.subscribe(listOf("im-topic"))
    var running = true
    while (running) {
        val records = consumer.poll(Duration.ofMillis(1.seconds.inWholeMilliseconds))
        for (record in records) {
            println("Consumed message: ${record.value()} from partition: ${record.partition()}")
            if (record.value() == "stop") {
                running = false
            }
        }
    }
}