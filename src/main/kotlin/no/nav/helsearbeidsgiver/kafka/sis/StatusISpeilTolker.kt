package no.nav.helsearbeidsgiver.kafka.sis

import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class StatusISpeilTolker(
    private val soeknadRepository: SoeknadRepository,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val behandlingstatusmelding = melding.fromJson(Behandlingstatusmelding.serializer())
            logger.info(
                "Mottok status-i-speil-melding med status ${behandlingstatusmelding.status}, " +
                    "vedtaksperiodeId ${behandlingstatusmelding.vedtaksperiodeId} og eksterneSøknadIder " +
                    "${behandlingstatusmelding.eksterneSøknadIder}.",
            )
            if (behandlingstatusmelding.status == Behandlingstatusmelding.Behandlingstatustype.OPPRETTET) {
                // Hvis en søknad finnes fra før i databasen med en vedtaksperiodeId logg erroor/ warning, men oppdater
                // de resterende søknadene med vedtaksperiodeid
                // Lagre vedtaksperiodeId på tilhørende søknader
                // TODO: hvis søknad consumeren går ned vil det ikke finnes en søknad å oppdatere. Hvordan håndtere dette?
                logger.info(
                    "Oppdater søknader ${behandlingstatusmelding.eksterneSøknadIder} med vedtaksperiodeId ${behandlingstatusmelding.vedtaksperiodeId}",
                )
                soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(
                    behandlingstatusmelding.eksterneSøknadIder,
                    behandlingstatusmelding.vedtaksperiodeId,
                )
            }
        } catch (e: Exception) {
            val errorMsg = "Klarte ikke å lagre status-i-speil-melding!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw e
        }
    }
}
