package no.nav.helsearbeidsgiver.kafka

import no.nav.helsearbeidsgiver.Env
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

fun createKafkaConsumerConfig(): Properties {
    val props = Properties()
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = Env.getPropertyOrNull("KAFKA_BROKERS") ?: "localhost:9092"
    props[ConsumerConfig.GROUP_ID_CONFIG] = Env.getPropertyOrNull("KAFKA_GROUP_ID") ?: "helsearbeidsgiver-sykepenger-im-lps-api-v1"
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
    props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
    return props
}
