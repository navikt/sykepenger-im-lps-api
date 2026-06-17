package no.nav.helsearbeidsgiver.csvimport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.utils.LeaderConfig
import no.nav.helsearbeidsgiver.utils.NaisLeaderConfig
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val log = LoggerFactory.getLogger("NattligCsvJobb")
private val KJORETIDSPUNKT: LocalTime = LocalTime.of(23, 0)

class NattligCsvJobb(
    private val forespoerselRepository: ForespoerselRepository,
    private val leaderConfig: LeaderConfig = NaisLeaderConfig,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    delayMillis: Long = 60 * 1000L,
) : RecurringJob(coroutineScope, delayMillis) {
    private var sistKjortDato: LocalDate? = null

    @Synchronized
    override fun doJob() {
        if (!leaderConfig.isElectedLeader()) return

        val na = LocalDateTime.now()
        val iDag = na.toLocalDate()

        if (sistKjortDato == iDag) return
        if (na.toLocalTime().isBefore(KJORETIDSPUNKT)) return

        runCatching {
            log.info("Kjorer nattlig CSV-jobb")
            CsvLeser(forespoerselRepository = forespoerselRepository).reaktiveNavReferanseIder()
            sistKjortDato = iDag
            log.info("Nattlig CSV-jobb fullfort")
        }.onFailure {
            log.error("Feil i nattlig CSV-jobb", it)
        }
    }
}
