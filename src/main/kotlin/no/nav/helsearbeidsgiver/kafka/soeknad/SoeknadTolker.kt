package no.nav.helsearbeidsgiver.kafka.soeknad

import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.soeknad.SoeknadService
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SoeknadTolker(
    private val soeknadService: SoeknadService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val soeknadMessage = melding.fromJson(SykepengesoknadDTO.serializer())
            sikkerLogger.info(
                "Mottok søknad med id ${soeknadMessage.id}, sykmeldingId ${soeknadMessage.sykmeldingId} og sendtNav ${soeknadMessage.sendtNav}.",
            )
            soeknadService.behandleSoeknad(soeknadMessage)
        } catch (e: Exception) {
            val errorMsg = "Klarte ikke å lagre søknad om sykepenger!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw e
        }
    }
}
