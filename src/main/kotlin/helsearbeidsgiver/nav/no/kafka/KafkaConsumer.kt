package helsearbeidsgiver.nav.no.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun startKafkaConsumer() {
    val consumer = KafkaConsumer<String, String>(createKafkaConsumerConfig() as Map<String, Any>)
    consumer.subscribe(listOf("im-topic"))

    runBlocking {
        launch(Dispatchers.IO) {
            var running = true
            while (running) {
                Thread.sleep(5.seconds.inWholeMilliseconds)
                val records = consumer.poll(Duration.ofMillis(100))
                for (record in records) {
                    println("Consumed message: ${record.value()} from partition: ${record.partition()}")
                    if (record.value() == "stop") {
                        running = false
                    }
                }


            }
        }
    }
}