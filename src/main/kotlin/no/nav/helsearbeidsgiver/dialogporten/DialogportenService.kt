package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger

class DialogportenService(
    val dialogProducer: DialogProducer,
    val unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()

    fun opprettNyDialogMedSykmelding(sykmelding: DialogSykmelding) {
        if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(sykmelding.orgnr)) {
            dialogProducer.send(sykmelding)
            logger.info(
                "Sendte melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId}",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId} fordi feature toggle er av.",
            )
        }
    }

    fun oppdaterDialogMedSykepengesoknad(soknad: DialogSykepengesoknad) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattSoknad(soknad.orgnr)) {
            dialogProducer.send(soknad)
            logger.info(
                "Sendte melding til hag-dialog for sykepengesøknad med søknadId: ${soknad.soknadId}, sykmeldingId: ${soknad.sykmeldingId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for sykepengesøknad med søknadId: ${soknad.soknadId}, sykmeldingId: ${soknad.sykmeldingId}, på fordi feature toggle er av.",
            )
        }
    }
}
