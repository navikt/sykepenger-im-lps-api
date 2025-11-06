package no.nav.helsearbeidsgiver.utils

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.log.logger
import java.net.InetAddress

interface LeaderConfig {
    fun isElectedLeader(): Boolean
}

class NaisLeaderElectionConfig : LeaderConfig {
    private val unknownLeader = "UNKNOWN_LEADER"
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
                    logger().warn("feilet å hente elected leader", e)
                    unknownLeader
                }
            } else {
                logger().warn("ELECTOR_GET_URL er null")
                getHostName() ?: unknownLeader
            }
        }

    private fun getHostName(): String? = InetAddress.getLocalHost().hostName

    @Serializable
    data class ElectedPod(
        val name: String,
    )
}
