package no.nav.helsearbeidsgiver.kafka.refusjon

import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfall
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfallRepository
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

        val ider = "refusjonUtfallId ${refusjonUtfall.refusjonUtfallId} og vedtaksperiodeId ${refusjonUtfall.vedtaksperiodeId}"
        logger.info("Mottok refusjonsutfall med $ider.")
        sikkerLogger.info("Mottok refusjonsutfall: $refusjonUtfall")

        val bleLagret = refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        if (bleLagret) {
            logger.info("Lagret refusjonsutfall med $ider.")
        } else {
            logger.warn("Refusjonsutfall med $ider er allerede lagret, ignorerer duplikat.")
        }
    }
}
