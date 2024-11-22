package no.nav.helsearbeidsgiver.kafka

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

suspend fun startKafkaConsumer(
    topic: String,
    consumer: KafkaConsumer<String, String>,
    meldingTolker: MeldingTolker,
) {
    consumer.subscribe(listOf(topic))
    consumer.asFlow().collect { record ->
        try {
            meldingTolker.lesMelding(record.value())
            consumer.commitSync()
        } catch (e: Exception) {
            sikkerLogger().error("Feil ved polling / lagring, avslutter!", e)
            // TODO; Forsøk igjen noen ganger først, disable evt lesing fra kafka i en periode.
            // Kan evt restarte med en gang, hvis vi har flere noder (restart går utover API ellers)
            throw e // Kaster slik at kubernetes vil fange og restarte poden.
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
