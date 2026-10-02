package no.nav.helsearbeidsgiver.kafka.refusjon

import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.refusjonUtfall.RefusjonUtfall
import no.nav.helsearbeidsgiver.refusjonUtfall.RefusjonUtfallRepository
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class RefusjonUtfallTolker(
    private val refusjonUtfallRepository: RefusjonUtfallRepository,
) : MeldingTolker {
    private val logger = logger()
    private val sikkerLogger = sikkerLogger()

    override fun lesMelding(melding: String) {
        val refusjonUtfall =
            try {
                melding.fromJson(RefusjonUtfall.serializer())
            } catch (e: SerializationException) {
                logger.error("Klarte ikke å lese refusjonsutfall-melding, feil format.")
                sikkerLogger.error("Klarte ikke å lese refusjonsutfall-melding, feil format. melding=$melding", e)
                throw e
            }

        val logIdInfo = "refusjonUtfallId ${refusjonUtfall.refusjonUtfallId} og vedtaksperiodeId ${refusjonUtfall.vedtaksperiodeId}"
        logger.info("Mottok refusjonsutfall med $logIdInfo.")
        sikkerLogger.info("Mottok refusjonsutfall: $refusjonUtfall")

        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        logger.info("Lagret refusjonsutfall med $logIdInfo.")
    }
}
