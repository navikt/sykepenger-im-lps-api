package no.nav.helsearbeidsgiver

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

private val appConfig = HoconApplicationConfig(ConfigFactory.load())

object Env {
    fun getProperty(prop: String): String {
        return System.getProperty(prop) ?: appConfig.property(prop).getString()
    }

    fun getPropertyOrNull(prop: String): String? {
        return System.getProperty(prop) ?: appConfig.propertyOrNull(prop)?.getString()
    }
}
