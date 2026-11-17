package no.nav.helsearbeidsgiver.kafka.refusjon

import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfall
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfallRepository
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class RefusjonUtfallTolker(
    private val refusjonUtfallRepository: RefusjonUtfallRepository,
    private val dokumentkoblingService: DokumentkoblingService,
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

        val nyttUtfall = refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        if (nyttUtfall) {
            "Lagret refusjonsutfall med $logIdInfo.".also {
                logger.info(it)
                sikkerLogger.info(it)
            }
        } else {
            "Refusjonsutfall med $logIdInfo er allerede lagret, sender kobling på nytt.".also {
                logger.info(it)
                sikkerLogger.info(it)
            }
        }

        dokumentkoblingService.produserRefusjonKobling(refusjonUtfall)
    }
}
