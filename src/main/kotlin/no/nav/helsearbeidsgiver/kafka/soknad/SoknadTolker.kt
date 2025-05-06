package no.nav.helsearbeidsgiver.kafka.soknad

import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
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
            sikkerLogger().info("Mottok søknad på kafka: $soknadMessage.")
            // TODO: Lagre søknad i database
        } catch (e: Exception) {
            "Klarte ikke å lagre søknad om sykepenger!".also {
                logger.error(it)
                sikkerLogger.error(it, e)
            }
            throw e
        }
    }
}
