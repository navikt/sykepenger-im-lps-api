package no.nav.helsearbeidsgiver.bakgrunnsjobb

import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.innsending.InnsendingRepository
import no.nav.helsearbeidsgiver.utils.getElectedLeaderId
import no.nav.helsearbeidsgiver.utils.getHostName
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class InnsendingProcessor(
    val innsendingRepository: InnsendingRepository,
) : BakgrunnsjobbProsesserer {
    companion object {
        const val JOB_TYPE = "innsendingsjobb"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        sikkerLogger().info("Prosesserer ${jobb.uuid} med type ${jobb.type} på ${getHostName()} elected leader: ${getElectedLeaderId()}")
        sikkerLogger().info("Data: $jobb")
        sikkerLogger().info(
            @Suppress("ktlint:standard:max-line-length")
            "jobb status: ${jobb.status} og forsøk: ${jobb.forsoek} og data: ${jobb.data} og kjoeretid: ${jobb.kjoeretid} og opprettet: ${jobb.opprettet}  og behandlet: ${jobb.behandlet}",
        )
    }
}
