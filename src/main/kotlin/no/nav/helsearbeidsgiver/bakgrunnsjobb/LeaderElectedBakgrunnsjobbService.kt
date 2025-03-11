package no.nav.helsearbeidsgiver.bakgrunnsjobb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.utils.getElectedLeaderId
import no.nav.helsearbeidsgiver.utils.isElectedLeader
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.LocalDateTime

class LeaderElectedBakgrunnsjobbService(
    delayMillis: Long = 30 * 1000L,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    bakgrunnsjobbRepository: BakgrunnsjobbRepository,
) : RecurringJob(coroutineScope, delayMillis) {
    val bakgrunnsjobbService = BakgrunnsjobbService(bakgrunnsjobbRepository)

    inline fun <reified T : BakgrunnsjobbProsesserer> opprettJobb(
        kjoeretid: LocalDateTime = LocalDateTime.now(),
        forsoek: Int = 0,
        maksAntallForsoek: Int = 3,
        data: String,
    ) {
        bakgrunnsjobbService.opprettJobb<T>(
            kjoeretid = kjoeretid,
            forsoek = forsoek,
            maksAntallForsoek = maksAntallForsoek,
            data = data,
        )
    }

    fun registrer(prosesserer: BakgrunnsjobbProsesserer) {
        bakgrunnsjobbService.registrer(prosesserer)
    }

    override fun doJob() {
        if (isElectedLeader()) {
            do {
                val wasEmpty =
                    bakgrunnsjobbService
                        .finnVentende()
                        .also { logger.debug("Bakgrunnsjobb: Fant ${it.size} bakgrunnsjobber å kjøre på ${getElectedLeaderId()}") }
                        .onEach { bakgrunnsjobbService.prosesser(it) }
                        .isEmpty()
            } while (!wasEmpty)
        } else {
            sikkerLogger().info("Bakgrunnsjobb: Ikke leder, venter til neste runde")
        }
    }
}
