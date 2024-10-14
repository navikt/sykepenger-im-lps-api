package no.nav.helsearbeidsgiver.kafka.inntecktsmelding

import no.nav.helsearbeidsgiver.kafka.KafkaInnkommendeMeldingService
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class InntektsmeldingKafkaConsumer : LpsKafkaConsumer {
    private val logger = LoggerFactory.getLogger(KafkaInnkommendeMeldingService::class.java)

    override fun handleRecord(record: ConsumerRecord<String, String>) {
        logger.info("Received record: ${record.value()} from topic: ${record.topic()} with key: ${record.key()}")
    }
}
