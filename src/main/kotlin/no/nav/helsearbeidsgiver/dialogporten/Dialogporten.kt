package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

interface IDialogportenService {
    fun opprettNyDialogMedSykmelding(dialog: DialogSykmelding)
}

class IngenDialogportenService : IDialogportenService {
    override fun opprettNyDialogMedSykmelding(dialog: DialogSykmelding) {
        val generertId = UUID.randomUUID()
        sikkerLogger().info("Oppretter ikke dialog for sykmelding generertId: $generertId")
    }
}

class DialogportenService(
    val dialogProducer: DialogProducer,
) : IDialogportenService {
    override fun opprettNyDialogMedSykmelding(dialog: DialogSykmelding) {
        dialogProducer.send(dialog)
        sikkerLogger().info("Sender dialog for sykmelding med sykmeldingId: ${dialog.sykmeldingId}, på orgnr: ${dialog.orgnr}")
    }
}
