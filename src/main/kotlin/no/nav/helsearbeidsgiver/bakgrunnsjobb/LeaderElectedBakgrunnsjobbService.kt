package no.nav.helsearbeidsgiver.bakgrunnsjobb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.net.InetAddress
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
            sikkerLogger().debug("Bakgrunnsjobb: Ikke leder, venter til neste runde")
        }
    }
}

fun getElectedLeaderId(): String =
    runBlocking {
        val electorUrl = getPropertyOrNull("ELECTOR_GET_URL")
        sikkerLogger().info("Hentet elector url: $electorUrl")

        if (electorUrl != null) {
            try {
                val electedPod: ElectedPod = createHttpClient().get(electorUrl).body()
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

@Serializable
data class ElectedPod(
    val name: String,
)

fun isElectedLeader(): Boolean {
    val electedLeaderId = getElectedLeaderId()
    val hostName = getHostName()
    sikkerLogger().info("HOST NAME: $hostName")
    return electedLeaderId == hostName
}

fun getHostName(): String? = InetAddress.getLocalHost().hostName

fun createHttpClient() =
    HttpClient(Apache5) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }
