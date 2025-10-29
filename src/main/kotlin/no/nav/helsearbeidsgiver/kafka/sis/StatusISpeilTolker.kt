package no.nav.helsearbeidsgiver.kafka.sis

import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.sis.StatusISpeilRepository
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class StatusISpeilTolker(
    private val soeknadRepository: SoeknadRepository,
    private val statusISpeilRepository: StatusISpeilRepository,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val behandlingstatusmelding = melding.fromJson(Behandlingstatusmelding.serializer())
            logger.debug(
                "Mottok status-i-speil-melding med status ${behandlingstatusmelding.status}, " +
                    "vedtaksperiodeId ${behandlingstatusmelding.vedtaksperiodeId} og eksterneSøknadIder " +
                    "${behandlingstatusmelding.eksterneSøknadIder}.",
            )
            if (behandlingstatusmelding.status == Behandlingstatusmelding.Behandlingstatustype.OPPRETTET) {
                if (behandlingstatusmelding.eksterneSøknadIder == null) {
                    logger.warn(
                        "VedtaksperiodeId: ${behandlingstatusmelding.vedtaksperiodeId} har status ${behandlingstatusmelding.status} - kan ikke kombineres med eksterneSøknadIder=null, ignorerer meldingen",
                    )
                    return
                } else {
                    logger.info(
                        "Oppdater søknader ${behandlingstatusmelding.eksterneSøknadIder} med vedtaksperiodeId ${behandlingstatusmelding.vedtaksperiodeId}",
                    )
                    soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(
                        behandlingstatusmelding.eksterneSøknadIder,
                        behandlingstatusmelding.vedtaksperiodeId,
                    )
                    statusISpeilRepository.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding)
                }
            }
        } catch (serializationException: SerializationException) {
            sikkerLogger.error("Feil format på Behandlingstatusmelding, melding=$melding", serializationException)
            throw serializationException
        } catch (e: Exception) {
            val errorMsg = "Klarte ikke å lagre status-i-speil-melding!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw e
        }
    }
}
