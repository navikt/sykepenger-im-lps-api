package no.nav.helsearbeidsgiver.kafka

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

suspend fun startKafkaConsumer(
    topic: String,
    consumer: KafkaConsumer<String, String>,
    meldingTolker: MeldingTolker,
    enabled: () -> Boolean = { true },
) {
    val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)
    consumer.subscribe(listOf(topic))
    consumer.asFlow({ consumer.toggleConsumer(enabled, topic) }).collect { record ->
        try {
            if (!enabled()) {
                return@collect
            }
            // Obs: record.value() kan være null fordi det er implementert i Java
            when (val value: String? = record.value()) {
                null ->
                    logger.warn(
                        "Mottok melding med null som value, ignorerer melding med offset: ${record.offset()}, key: ${record.key()}",
                    )
                else -> {
                    if (record.timestamp().erInnenforSiste2Aar()) {
                        meldingTolker.lesMelding(value)
                    } else {
                        logger.warn(
                            "Mottok melding som er eldre enn 2 år, ignorerer melding på topic $topic med offset: ${record.offset()} " +
                                "og timestamp: ${Instant.ofEpochMilli(record.timestamp())}",
                        )
                    }
                }
            }
            consumer.commitSync()
        } catch (e: Exception) {
            "Feil ved polling / lagring, avslutter! Pod må restartes! Topic = $topic, KafkaPartition = ${record.partition()} og Offset = ${record.offset()}"
                .let {
                    logger.error(it)
                    sikkerLogger().error(it, e)
                }
            // TODO; Forsøk igjen noen ganger først, disable evt lesing fra kafka i en periode.
            // Kan evt restarte med en gang, hvis vi har flere noder (exit går utover API ellers)
            throw e
        }
    }
}

fun <K, V> KafkaConsumer<K, V>.toggleConsumer(
    enabled: () -> Boolean,
    topic: String,
) {
    val konsumeringPauset = this.paused().isNotEmpty()
    if (!enabled() && !konsumeringPauset) {
        logger().warn("Pauser konsumering av topic $topic}")
        this.pause(this.assignment())
    } else if (enabled() && konsumeringPauset) {
        logger().warn("Gjenopptar konsumering av topic $topic}")
        this.resume(this.assignment())
    }
}

fun <K, V> KafkaConsumer<K, V>.asFlow(
    toggleSjekk: () -> Unit,
    timeout: Duration = Duration.ofMillis(10),
): Flow<ConsumerRecord<K, V>> =
    flow {
        while (true) {
            toggleSjekk()
            poll(timeout).forEach { emit(it) }
        }
    }

fun Long.erInnenforSiste2Aar(): Boolean = this > Instant.now().minus(365 * 2, ChronoUnit.DAYS).toEpochMilli()

interface MeldingTolker {
    fun lesMelding(melding: String)
}
