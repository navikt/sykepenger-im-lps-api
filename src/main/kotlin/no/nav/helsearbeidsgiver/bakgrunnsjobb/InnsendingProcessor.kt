package no.nav.helsearbeidsgiver.bakgrunnsjobb

import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.innsending.InnsendingRepository
import no.nav.helsearbeidsgiver.utils.isElectedLeader
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class InnsendingProcessor(
    val innsendingRepository: InnsendingRepository,
) : BakgrunnsjobbProsesserer {
    companion object {
        const val JOB_TYPE = "innsendingsjobb"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        sikkerLogger().info("Prosesserer ${jobb.uuid} med type ${jobb.type}")
        if (isElectedLeader()) {
            sikkerLogger().info("Prosesserer kun for denne noden  ${jobb.uuid} med type ${jobb.type}")
        } else {
            sikkerLogger().info("Prosesserer ikke for denne noden  ${jobb.uuid} med type ${jobb.type}")
        }
    }
}
