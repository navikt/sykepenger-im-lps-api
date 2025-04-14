package no.nav.helsearbeidsgiver
import no.nav.helsearbeidsgiver.utils.TestData.SYKMELDING_MOTTATT
import no.nav.helsearbeidsgiver.utils.buildForespoerselMottattJson
import no.nav.helsearbeidsgiver.utils.buildInntektsmeldingDistribuertJson
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

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

object Producer {
    val props =
        Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }
    val kafkaProducer = KafkaProducer<String, String>(props)

    fun sendMelding(record: ProducerRecord<String, String>) {
        kafkaProducer.send(record) { metadata, exception ->
            if (exception != null) {
                println("Error sending message: ${exception.message}")
            } else {
                println("Message sent to topic ${metadata.topic()} partition ${metadata.partition()} with offset ${metadata.offset()}")
            }
        }
    }
}
