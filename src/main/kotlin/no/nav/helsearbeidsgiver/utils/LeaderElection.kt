package no.nav.helsearbeidsgiver.utils

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.log.logger
import java.net.InetAddress

object LeaderElection {
    const val UNKNOWN_LEADER = "UNKNOWN_LEADER"
    private val httpClient = createHttpClient()

    fun isElectedLeader(): Boolean {
        val electedLeaderId = getElectedLeaderId()
        val hostName = getHostName()
        return electedLeaderId == hostName
    }

    private fun getElectedLeaderId(): String =
        runBlocking {
            if (leaderElectionEnabled()) {
                val electorUrl = getPropertyOrNull("ELECTOR_GET_URL")
                if (electorUrl != null) {
                    try {
                        val electedPod: ElectedPod = httpClient.get(electorUrl).body()
                        logger().debug("Elected leader: ${electedPod.name} and host: ${getHostName()}")
                        electedPod.name
                    } catch (e: Exception) {
                        logger().warn("feilet å hente elected leader", e)
                        UNKNOWN_LEADER
                    }
                } else {
                    logger().warn("ELECTOR_GET_URL er null")
                    UNKNOWN_LEADER
                }
            } else {
                logger().debug("Leader election is disabled")
                getHostName() ?: UNKNOWN_LEADER
            }
        }

    private fun leaderElectionEnabled(): Boolean = getPropertyOrNull("application.leaderElection.enabled")?.toBoolean() ?: false

    private fun getHostName(): String? = InetAddress.getLocalHost().hostName

    @Serializable
    data class ElectedPod(
        val name: String,
    )
}
