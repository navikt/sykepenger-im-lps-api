package no.nav.helsearbeidsgiver.kafka.soknad

import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.soknad.SoknadService
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SoknadTolker(
    private val soknadService: SoknadService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val soknadMessage = melding.fromJson(SykepengesoknadDTO.serializer())
            sikkerLogger.info(
                "Mottok søknad med id ${soknadMessage.id}, sykmeldingId ${soknadMessage.sykmeldingId} og sendtNav ${soknadMessage.sendtNav}.",
            )
            soknadService.behandleSoknad(soknadMessage)
        } catch (e: Exception) {
            val errorMsg = "Klarte ikke å lagre søknad om sykepenger!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw e
        }
    }
}
