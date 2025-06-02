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
                "Sendte melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId}, på orgnr: ${sykmelding.orgnr}",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId}, på orgnr: ${sykmelding.orgnr} fordi feature toggle er av.",
            )
        }
    }

    fun oppdaterDialogMedSykepengesoknad(soknad: DialogSykepengesoknad) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattSoknad(soknad.orgnr)) {
            dialogProducer.send(soknad)
            logger.info(
                "Sendte melding til hag-dialog for sykepengesøknad med søknadId: ${soknad.soknadId}, sykmeldingId: ${soknad.sykmeldingId}, på orgnr: ${soknad.orgnr}",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for sykepengesøknad med søknadId: ${soknad.soknadId}, sykmeldingId: ${soknad.sykmeldingId}, på orgnr: ${soknad.orgnr} fordi feature toggle er av.",
            )
        }
    }
}
