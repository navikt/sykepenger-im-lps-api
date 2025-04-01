package no.nav.helsearbeidsgiver

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureAuth
import no.nav.helsearbeidsgiver.config.configureKafkaConsumers
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.plugins.configureRouting
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun main() {
    startServer()
}

fun startServer() {
    sikkerLogger().info("Setter opp database...")
    val db = DbConfig.init()
    sikkerLogger().info("Setter opp repositories og services...")
    val repositories = configureRepositories(db)
    val services = configureServices(repositories)
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            apiModule(services = services)
            configureKafkaConsumers(services = services, repositories = repositories)
        },
    ).start(wait = true)
}

fun Application.apiModule(services: Services) {
    logger().info("Starter applikasjon!")
    install(ContentNegotiation) {
        json()
    }
    logger().info("Setter opp autentisering...")
    configureAuth()

    logger().info("Setter opp ruting...")
    configureRouting(services)
}
