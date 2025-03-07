package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.net.InetAddress

fun getElectedLeaderId(): String =
    runBlocking {
        val electorUrl = getPropertyOrNull("ELECTOR_GET_URL")
        logger().info("Hentet elector url: $electorUrl")

        if (electorUrl != null) {
            try {
                val electedPod: ElectedPod = createHttpClient().get(electorUrl).body()
                logger().info("Elected leader: ${electedPod.name}")
                electedPod.name
            } catch (e: Exception) {
                logger().warn("feilet å hente elected leader", e)
                "UNKNOWN_LEADER"
            }
        } else {
            logger().warn("ELECTOR_GET_URL er null")
            "UNKNOWN_LEADER"
        }
    }

data class ElectedPod(
    val name: String,
)

fun isElectedLeader(): Boolean {
    val electedLeaderId = getElectedLeaderId()
    val hostName = InetAddress.getLocalHost().hostName
    sikkerLogger().info("HOST NAME: $hostName")
    return electedLeaderId == hostName
}

fun createHttpClient() =
    HttpClient(Apache5) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }
