package no.nav.helsearbeidsgiver.kafka.sis

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.sis.StatusISpeilRepository
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class StatusISpeilTolker(
    private val soeknadRepository: SoeknadRepository,
    private val statusISpeilRepository: StatusISpeilRepository,
    private val dokumentkoblingService: DokumentkoblingService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            // Vi antar behandlingstatus-melding dersom vi ikke får noe EventName
            when (melding.fromJson(EventNameWrapper.serializer()).eventName ?: SisEventName.BEHANDLINGSTATUS) {
                SisEventName.VEDTAK_FATTET -> parseVedtakArbeidsgiverMelding(melding.fromJson(VedtakArbeidsgiverMelding.serializer()))
                SisEventName.BEHANDLINGSTATUS -> parseBehandlingstatusmelding(melding.fromJson(Behandlingstatusmelding.serializer()))
            }
        } catch (serializationException: SerializationException) {
            sikkerLogger.error("Feil format på melding, melding=$melding", serializationException)
            throw serializationException
        } catch (e: Exception) {
            val errorMsg = "Klarte ikke å lagre status-i-speil-melding!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw e
        }
    }

    private fun parseVedtakArbeidsgiverMelding(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        if (vedtakArbeidsgiverMelding.yrkesaktivitetstype != Yrkesaktivitetstype.ARBEIDSTAKER) {
            logger.warn(
                "Ignorerer vedtak for vedtaksperiodeId ${vedtakArbeidsgiverMelding.vedtaksperiodeId} " +
                    "med yrkesaktivitetstype ${vedtakArbeidsgiverMelding.yrkesaktivitetstype}, kun ARBEIDSTAKER støttes.",
            )
            return
        }
        logger.info("Leste vedtak")
        sikkerLogger.info("Leste vedtak: $vedtakArbeidsgiverMelding")
    }

    private fun parseBehandlingstatusmelding(behandlingstatusmelding: Behandlingstatusmelding) {
        logger.debug(
            "Mottok status-i-speil-melding med status {}, vedtaksperiodeId {} og eksterneSøknadIder {}.",
            behandlingstatusmelding.status,
            behandlingstatusmelding.vedtaksperiodeId,
            behandlingstatusmelding.eksterneSøknadIder,
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

                behandlingstatusmelding.eksterneSøknadIder.forEach { soeknadId ->
                    dokumentkoblingService.produserVedtaksperiodeSoeknadKobling(
                        vedtaksperiodeId = behandlingstatusmelding.vedtaksperiodeId,
                        soeknadId = soeknadId,
                    )
                }

                soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(
                    behandlingstatusmelding.eksterneSøknadIder,
                    behandlingstatusmelding.vedtaksperiodeId,
                )

                statusISpeilRepository.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding)
            }
        }
    }
}

@Serializable
private data class EventNameWrapper(
    val eventName: SisEventName? = null,
)
