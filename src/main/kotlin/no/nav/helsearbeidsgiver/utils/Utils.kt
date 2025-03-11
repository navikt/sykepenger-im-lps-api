package no.nav.helsearbeidsgiver.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.net.InetAddress

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
