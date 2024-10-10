package helsearbeidsgiver.nav.no

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

private val appConfig = HoconApplicationConfig(ConfigFactory.load())

object Env {
    fun getProperty(prop: String): String = appConfig.property(prop).getString()
}

