package no.nav.helsearbeidsgiver.kafka

import no.nav.helsearbeidsgiver.Env
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

fun createKafkaConsumerConfig(consumerName: String): Properties {
    val pkcs12 = "PKCS12"
    val javaKeyStore = "jks"
    return Properties().apply {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Env.getPropertyOrNull("KAFKA_BROKERS") ?: "localhost:9092")
        put(ConsumerConfig.GROUP_ID_CONFIG, Env.getPropertyOrNull("KAFKA_GROUP_ID") ?: "helsearbeidsgiver-sykepenger-im-lps-api-v1")
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1")
        put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000")
        put(ConsumerConfig.CLIENT_ID_CONFIG, "sykepenger-im-lps-api-$consumerName")

        Env.getPropertyOrNull("KAFKA_TRUSTSTORE_PATH")?.let {
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, javaKeyStore)
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, pkcs12)
        }

        Env.getPropertyOrNull("KAFKA_CREDSTORE_PASSWORD")?.let {
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it)
            put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, it)
        }

        Env.getPropertyOrNull("KAFKA_KEYSTORE_PATH")?.let {
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it)
        }
    }
}

fun createKafkaProducerConfig(producerName: String): Properties {
    val pkcs12 = "PKCS12"
    val javaKeyStore = "jks"
    return Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Env.getPropertyOrNull("KAFKA_BROKERS") ?: "localhost:9092")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
        put(ProducerConfig.RETRIES_CONFIG, "2")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.CLIENT_ID_CONFIG, "sykepenger-im-lps-api-$producerName")

        Env.getPropertyOrNull("KAFKA_TRUSTSTORE_PATH")?.let {
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, javaKeyStore)
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, pkcs12)
        }

        Env.getPropertyOrNull("KAFKA_CREDSTORE_PASSWORD")?.let {
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it)
            put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, it)
        }

        Env.getPropertyOrNull("KAFKA_KEYSTORE_PATH")?.let {
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it)
        }
    }
}
