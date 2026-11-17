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
                "Klarte ikke å lese refusjonsutfall-melding, feil format.".also {
                    logger.error(it)
                    sikkerLogger.error("$it melding=$melding", e)
                }
                throw e
            }

        val logIdInfo = "refusjonUtfallId ${refusjonUtfall.refusjonUtfallId} og vedtaksperiodeId ${refusjonUtfall.vedtaksperiodeId}"
        "Mottok refusjonsutfall med $logIdInfo.".also {
            logger.info(it)
            sikkerLogger.info(it)
        }

        when (refusjonUtfallRepository.hentRefusjonUtfallMetadata(refusjonUtfall.refusjonUtfallId)) {
            null -> {
                refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
                "Lagret refusjonsutfall med $logIdInfo.".also {
                    logger.info(it)
                    sikkerLogger.info(it)
                }
            }

            else -> {
                "Refusjonsutfall med $logIdInfo finnes fra før og vil derfor ikke lagres på nytt.".also {
                    logger.warn(it)
                    sikkerLogger.warn(it)
                }
            }
        }

        dokumentkoblingService.produserRefusjonKobling(refusjonUtfall)
    }
}
