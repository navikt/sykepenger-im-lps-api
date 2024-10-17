package no.nav.helsearbeidsgiver.kafka.inntecktsmelding

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.inntektsmelding.ExposedMottak
import no.nav.helsearbeidsgiver.inntektsmelding.ImMottakRepository
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class InntektsmeldingKafkaConsumer : LpsKafkaConsumer {
    private val logger = LoggerFactory.getLogger(InntektsmeldingKafkaConsumer::class.java)

    override fun handleRecord(record: ConsumerRecord<String, String>) {
        // TODO: parse og behandle melding
        logger.info("Received record: ${record.value()} from topic: ${record.topic()} with key: ${record.key()}")
        runBlocking {
            ImMottakRepository().opprett(ExposedMottak(record.value()))
        }
    }
}
