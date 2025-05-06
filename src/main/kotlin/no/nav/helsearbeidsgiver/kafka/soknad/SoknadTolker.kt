package no.nav.helsearbeidsgiver.kafka.soknad

import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger


class SoknadTolker : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()


    override fun lesMelding(melding: String) {
        try {
            val soknadMessage = jsonMapper.decodeFromString<SykepengesoknadDTO>(melding)
            sikkerLogger.info("Mottok søknad på kafka: $soknadMessage.")
            // TODO: Lagre søknad i database
        } catch (e: Exception) {
            val errorMsg = "Klarte ikke å lagre søknad om sykepenger!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw SerializationException(errorMsg, e)
        }
    }
}
