package no.nav.helsearbeidsgiver.kafka

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

suspend fun startKafkaConsumer(
    topic: String,
    consumer: KafkaConsumer<String, String>,
    meldingTolker: MeldingTolker,
) {
    val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)
    consumer.subscribe(listOf(topic))
    consumer.asFlow().collect { record ->
        try {
            sikkerLogger().info("Lagrer melding med offset: ${record.offset()}, key: ${record.key()} og value: ${record.value()}")
            meldingTolker.lesMelding(record.value())
            consumer.commitSync()
        } catch (e: Exception) {
            "Feil ved polling / lagring, avslutter!".let {
                logger.error(it)
                sikkerLogger().error(it, e)
            }
            // TODO; Forsøk igjen noen ganger først, disable evt lesing fra kafka i en periode.
            // Kan evt restarte med en gang, hvis vi har flere noder (exit går utover API ellers)
            throw e
        }
    }
}

fun <K, V> KafkaConsumer<K, V>.asFlow(timeout: Duration = Duration.ofMillis(10)): Flow<ConsumerRecord<K, V>> =
    flow {
        while (true) {
            poll(timeout).forEach { emit(it) }
        }
    }

interface MeldingTolker {
    fun lesMelding(melding: String)
}
