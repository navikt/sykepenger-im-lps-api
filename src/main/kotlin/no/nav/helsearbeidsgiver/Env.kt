package no.nav.helsearbeidsgiver

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

private val appConfig = HoconApplicationConfig(ConfigFactory.load())

object Env {
    fun getProperty(prop: String): String = System.getenv(prop) ?: appConfig.property(prop).getString()

    fun getPropertyAsList(prop: String): List<String> = (System.getenv(prop) ?: appConfig.property(prop).getString()).split(" ")

    fun getPropertyOrNull(prop: String): String? = System.getenv(prop) ?: appConfig.propertyOrNull(prop)?.getString()
}
