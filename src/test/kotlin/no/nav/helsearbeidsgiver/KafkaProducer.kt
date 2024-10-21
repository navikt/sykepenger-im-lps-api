import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.util.Properties

fun main() {
    val filePath = "inntektsmelding.json"
    val jsonFromFile = readJsonFromResources(filePath)

    val props =
        Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }

    val producer = KafkaProducer<String, String>(props)

    try {
        val record = ProducerRecord<String, String>("helsearbeidsgiver.rapid", "key", jsonFromFile)
        producer.send(record) { metadata, exception ->
            if (exception != null) {
                println("Error sending message: ${exception.message}")
            } else {
                println("Message sent to topic ${metadata.topic()} partition ${metadata.partition()} with offset ${metadata.offset()}")
            }
        }
    } finally {
        producer.close()
    }
}

fun readJsonFromResources(fileName: String): String {
    val resource = KafkaProducer::class.java.getResource("/$fileName")
    return File(resource.toURI()).readText(Charsets.UTF_8)
}
