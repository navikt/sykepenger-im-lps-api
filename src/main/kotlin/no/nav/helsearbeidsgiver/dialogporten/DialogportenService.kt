package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class DialogportenService(
    val dialogProducer: DialogProducer,
    val unleashFeatureToggles: UnleashFeatureToggles,
) {
    fun opprettNyDialogMedSykmelding(dialog: DialogSykmelding) {
        if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(dialog.orgnr)) {
            dialogProducer.send(dialog)
            sikkerLogger().info(
                "Sender melding til hag-dialog for sykmelding med sykmeldingId: ${dialog.sykmeldingId}, på orgnr: ${dialog.orgnr}",
            )
        } else {
            sikkerLogger().info(
                "Sender _ikke_ melding til hag-dialog for sykmelding med sykmeldingId: ${dialog.sykmeldingId}, på orgnr: ${dialog.orgnr}",
            )
        }
    }
}
