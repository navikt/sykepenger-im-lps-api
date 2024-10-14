package no.nav.helsearbeidsgiver.kafka

import org.slf4j.LoggerFactory

interface InnkommendeMeldingService {
    fun behandle(melding: String): Boolean
}

class KafkaInnkommendeMeldingService : InnkommendeMeldingService {
    private val logger = LoggerFactory.getLogger(KafkaInnkommendeMeldingService::class.java)

    // TODO: parse og behandle melding
    override fun behandle(melding: String): Boolean {
        logger.debug(melding)
        return true
    }
}
