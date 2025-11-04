package no.nav.helsearbeidsgiver.bakgrunnsjobb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonElement
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.utils.LeaderElection
import no.nav.helsearbeidsgiver.utils.log.logger
import java.time.LocalDateTime

class LeaderElectedBakgrunnsjobbService(
    val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
    delayMillis: Long = 30 * 1000L,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : RecurringJob(coroutineScope, delayMillis) {
    val bakgrunnsjobbService = BakgrunnsjobbService(bakgrunnsjobbRepository)

    inline fun <reified T : BakgrunnsjobbProsesserer> opprettJobb(
        kjoeretid: LocalDateTime = LocalDateTime.now(),
        forsoek: Int = 0,
        maksAntallForsoek: Int = 3,
        data: JsonElement,
    ) {
        bakgrunnsjobbService.opprettJobbJson<T>(
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
        if (LeaderElection.isElectedLeader()) {
            do {
                val jobbFunnet =
                    bakgrunnsjobbService
                        .finnVentende()
                        .also { logger().debug("Bakgrunnsjobb: Fant ${it.size} bakgrunnsjobber}") }
                        .onEach { bakgrunnsjobbService.prosesser(it) }
                        .isNotEmpty()
            } while (jobbFunnet)
        }
    }
}
