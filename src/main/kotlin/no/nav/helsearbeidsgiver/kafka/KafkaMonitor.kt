package no.nav.helsearbeidsgiver.kafka

import org.slf4j.LoggerFactory
import java.util.Collections

object KafkaMonitor {
    val logger = LoggerFactory.getLogger(KafkaMonitor::class.java)

    private val consumers: MutableMap<String, Boolean> = Collections.synchronizedMap(mutableMapOf<String, Boolean>())

    fun registrerConsumer(topic: String) {
        logger.info("Registrer consumer på topic: '$topic'")
        consumers.put(topic, false)
    }

    fun registrerFeil(topic: String) {
        logger.warn("Registrer feil på topic: '$topic'")
        consumers.put(topic, true)
    }

    fun harFeil(): Boolean = consumers.values.any { it }
}
