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
    val consumerKafkaProperties =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to
                resolveKafkaBrokers(),
            ConsumerConfig.GROUP_ID_CONFIG to (
                Env.getPropertyOrNull("KAFKA_GROUP_ID")
                    ?: "helsearbeidsgiver-sykepenger-im-lps-api-v1"
            ),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "30000",
            ConsumerConfig.CLIENT_ID_CONFIG to "sykepenger-im-lps-api-$consumerName",
        )
    return Properties().apply { putAll(consumerKafkaProperties + commonKafkaProperties()) }
}

fun createKafkaProducerConfig(producerName: String): Properties {
    val producerKafkaProperties =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to
                resolveKafkaBrokers(),
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "1",
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to "true",
            ProducerConfig.MAX_BLOCK_MS_CONFIG to "15000",
            ProducerConfig.RETRIES_CONFIG to "2",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            ProducerConfig.CLIENT_ID_CONFIG to "sykepenger-im-lps-api-$producerName",
        )

    return Properties().apply { putAll(producerKafkaProperties + commonKafkaProperties()) }
}

fun resolveKafkaBrokers() =
    when {
        System.getProperty("KAFKA_BOOTSTRAP_SERVERS") != null -> System.getProperty("KAFKA_BOOTSTRAP_SERVERS")
        Env.getPropertyOrNull("KAFKA_BROKERS") != null -> Env.getPropertyOrNull("KAFKA_BROKERS")
        else -> "localhost:9092"
    }

private fun commonKafkaProperties(): Map<String, String> {
    val pkcs12 = "PKCS12"
    val javaKeyStore = "jks"

    val truststoreConfig =
        Env
            .getPropertyOrNull("KAFKA_TRUSTSTORE_PATH")
            ?.let {
                mapOf(
                    SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to it,
                    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
                    SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
                    SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to javaKeyStore,
                    SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to pkcs12,
                )
            }.orEmpty()

    val credstoreConfig =
        Env
            .getPropertyOrNull("KAFKA_CREDSTORE_PASSWORD")
            ?.let {
                mapOf(
                    SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to it,
                    SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to it,
                    SslConfigs.SSL_KEY_PASSWORD_CONFIG to it,
                )
            }.orEmpty()

    val keystoreConfig =
        Env
            .getPropertyOrNull("KAFKA_KEYSTORE_PATH")
            ?.let {
                mapOf(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to it)
            }.orEmpty()

    return truststoreConfig + credstoreConfig + keystoreConfig
}
