package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class DialogportenService(
    val dialogProducer: DialogProducer,
    val unleashFeatureToggles: UnleashFeatureToggles,
) {
    fun opprettNyDialogMedSykmelding(sykmelding: DialogSykmelding) {
        if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(sykmelding.orgnr)) {
            dialogProducer.send(sykmelding)
            sikkerLogger().info(
                "Sender melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId}, på orgnr: ${sykmelding.orgnr}",
            )
        } else {
            sikkerLogger().info(
                "Sender _ikke_ melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId}, på orgnr: ${sykmelding.orgnr}",
            )
        }
    }

    fun oppdaterDialogMedSykepengesoknad(soknad: DialogSykepengesoknad) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattSoknad(soknad.orgnr)) {
            dialogProducer.send(soknad)
            sikkerLogger().info(
                "Sender melding til hag-dialog for sykepengesøknad med søknadId: ${soknad.soknadId}, sykmeldingId: ${soknad.sykmeldingId}, på orgnr: ${soknad.orgnr}",
            )
        } else {
            sikkerLogger().info(
                "Sender _ikke_ melding til hag-dialog for sykepengesøknad med søknadId: ${soknad.soknadId}, sykmeldingId: ${soknad.sykmeldingId}, på orgnr: ${soknad.orgnr}",
            )
        }
    }
}
