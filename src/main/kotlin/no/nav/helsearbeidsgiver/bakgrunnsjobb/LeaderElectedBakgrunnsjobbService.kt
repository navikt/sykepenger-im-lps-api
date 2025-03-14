package no.nav.helsearbeidsgiver.bakgrunnsjobb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.net.InetAddress
import java.time.LocalDateTime

class LeaderElectedBakgrunnsjobbService(
    val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
    val httpClient: HttpClient,
    delayMillis: Long = 30 * 1000L,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
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
            logger().debug("Bakgrunnsjobb: Ikke leder, venter til neste runde")
        }
    }

    private fun getElectedLeaderId(): String =
        runBlocking {
            val electorUrl = getPropertyOrNull("ELECTOR_GET_URL")
            sikkerLogger().info("Hentet elector url: $electorUrl")

            if (electorUrl != null) {
                try {
                    val electedPod: ElectedPod = httpClient.get(electorUrl).body()
                    sikkerLogger().info("Elected leader: ${electedPod.name} and host: ${getHostName()}")
                    electedPod.name
                } catch (e: Exception) {
                    sikkerLogger().warn("feilet å hente elected leader", e)
                    "UNKNOWN_LEADER"
                }
            } else {
                sikkerLogger().warn("ELECTOR_GET_URL er null")
                "UNKNOWN_LEADER"
            }
        }

    private fun isElectedLeader(): Boolean {
        val electedLeaderId = getElectedLeaderId()
        val hostName = getHostName()
        sikkerLogger().info("HOST NAME: $hostName")
        return electedLeaderId == hostName
    }

    private fun getHostName(): String? = InetAddress.getLocalHost().hostName

    @Serializable
    data class ElectedPod(
        val name: String,
    )
}
