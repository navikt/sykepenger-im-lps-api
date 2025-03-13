package no.nav.helsearbeidsgiver.bakgrunnsjobb

import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.innsending.InnsendingRepository
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

class InnsendingProcessor(
    val innsendingRepository: InnsendingRepository,
) : BakgrunnsjobbProsesserer {
    companion object {
        const val JOB_TYPE = "innsendingsjobb"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        sikkerLogger().info("Bakgrunnsjobb: Data: $jobb")
        val hentById = innsendingRepository.hentById(UUID.fromString(jobb.data))
        sikkerLogger().info("Bakgrunnsjobb: Henting av $hentById")
        hentById?.let {
            sikkerLogger().info("Bakgrunnsjobb: Fant innsending med id: ${it.innsendingId}")
            sikkerLogger().info("Bakgrunnsjobb: Status: ${it.status}")
        }
    }
}
