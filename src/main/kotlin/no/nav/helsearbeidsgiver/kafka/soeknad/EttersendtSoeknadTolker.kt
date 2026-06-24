package no.nav.helsearbeidsgiver.kafka.soeknad

import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.soeknad.SoeknadService
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class EttersendtSoeknadTolker(
    private val soeknadService: SoeknadService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val soeknadMessage = melding.fromJson(SykepengeSoeknadKafkaMelding.serializer())
            soeknadService.behandleEttersendtSoeknad(soeknadMessage)
        } catch (e: Exception) {
            val errorMsg = "Ettersendt søknad tolker Klarte ikke å lagre søknad om sykepenger!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw e
        }
    }
}
