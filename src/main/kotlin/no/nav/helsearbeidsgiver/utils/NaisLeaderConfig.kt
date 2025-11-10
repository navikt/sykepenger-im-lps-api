package no.nav.helsearbeidsgiver.utils

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.net.InetAddress

/*
  Brukes for å finne ut hvilken pod som er leder.
  For å overlate ledervalg til nais, setter man leaderElection: true i nais.yaml
  (https://doc.nais.io/services/leader-election/?h=leader)
  I tester kan man overstyre med getTestLeaderConfig(), default er false (ikke leader)
  I denne applikasjonen brukes valgt leder til å avgjøre hvem som skal gjøre hva:
  -Leder-podden er den eneste som kjører bakgrunnsjobber (*LeaderElectedBakgrunnsjobbService)
  -Leder-podden konsumerer IKKE Kafka-topics (ved feil i konsumering, vil appen kunne restarte). Da vil leder fortsatt være oppe og servere API-kall.
  -Du må med andre ord ha minst to pods for å kjøre applikasjonen.

 */
interface LeaderConfig {
    fun isElectedLeader(): Boolean
}

object NaisLeaderConfig : LeaderConfig {
    private const val UNKNOWN_LEADER = "UNKNOWN_LEADER"
    private val httpClient = createHttpClient()

    override fun isElectedLeader(): Boolean {
        val electedLeaderId = getElectedLeaderId()
        val hostName = getHostName()
        return electedLeaderId == hostName
    }

    private fun getElectedLeaderId(): String =
        runBlocking {
            val electorUrl = getPropertyOrNull("ELECTOR_GET_URL")
            if (electorUrl != null) {
                try {
                    val electedPod: ElectedPod = httpClient.get(electorUrl).body()
                    logger().debug("Elected leader: ${electedPod.name} and host: ${getHostName()}")
                    electedPod.name
                } catch (e: Exception) {
                    logger().error("feilet å hente elected leader")
                    sikkerLogger().error("feilet å hente elected leader", e)
                    UNKNOWN_LEADER
                }
            } else {
                logger().error("ELECTOR_GET_URL er null - er nais sidecar enablet?")
                UNKNOWN_LEADER
            }
        }

    private fun getHostName(): String? = InetAddress.getLocalHost().hostName

    @Serializable
    data class ElectedPod(
        val name: String,
    )
}
