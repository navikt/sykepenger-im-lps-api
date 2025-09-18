package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.utils.TestData.SYKMELDING_MOTTATT
import no.nav.helsearbeidsgiver.utils.buildForespoerselMottattJson
import no.nav.helsearbeidsgiver.utils.buildInntektsmeldingDistribuertJson
import no.nav.helsearbeidsgiver.utils.readJsonFromResources
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import no.nav.helsearbeidsgiver.kafka.resolveKafkaBrokers

fun main() {
    genererKafkaMeldinger()
    // genererOppdatertForespoerselMelding()
}

fun genererKafkaMeldinger() {
    val inntektsmeldingDistribuertJson = buildInntektsmeldingDistribuertJson()
    val forespoerselJson = buildForespoerselMottattJson()
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")
    val imTopic = Env.getProperty("kafkaConsumer.inntektsmelding.topic")
    val sykmeldingTopic = Env.getProperty("kafkaConsumer.sykmelding.topic")

    val imRecord = ProducerRecord(imTopic, "key", inntektsmeldingDistribuertJson)
    val priRecord = ProducerRecord(priTopic, "key", forespoerselJson)
    val sykmeldingRecord = ProducerRecord(sykmeldingTopic, "key", SYKMELDING_MOTTATT)

    Producer.sendMelding(imRecord)
    Producer.sendMelding(priRecord)
    Producer.sendMelding(sykmeldingRecord)
    Producer.kafkaProducer.close()
}

fun genererOppdatertForespoerselMelding() {
    val filePath = "json/forespoersel_oppdatert.json"
    val forespoerselJson = readJsonFromResources(filePath)
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")
    val priRecord = ProducerRecord(priTopic, "key", forespoerselJson)
    Producer.sendMelding(priRecord)
    Producer.kafkaProducer.close()
}

object Producer {
    lateinit var kafkaProducer: KafkaProducer<String, String>

    private fun createProducer(): KafkaProducer<String, String> {
        val props =
            Properties().apply {
                // Use the fixed resolve logic here too, for consistency
                val brokers = resolveKafkaBrokers()
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                // Add timeouts to prevent hangs (finite retries)
                put(ProducerConfig.RETRIES_CONFIG, 3.toString())
                put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "5000") // 5s timeout on send
                put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000")
                put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000")
            }
        return KafkaProducer<String, String>(props)
    }

    fun sendMelding(record: ProducerRecord<String, String>) {
        if (!::kafkaProducer.isInitialized) {
            kafkaProducer = createProducer()
        }
        kafkaProducer.send(record) { metadata, exception ->
            if (exception != null) {
                println("Error sending message: ${exception.message}")
                // Optional: Log brokers used for debug
//                println("Used brokers: ${kafkaProducer.props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG]}")
            } else {
                println("Message sent to topic ${metadata.topic()} partition ${metadata.partition()} with offset ${metadata.offset()}")
            }
        }
    }

    // Optional: Close for cleanup (call in @AfterAll if needed)
    fun close() {
        if (::kafkaProducer.isInitialized) {
            kafkaProducer.close()
        }
    }
}
