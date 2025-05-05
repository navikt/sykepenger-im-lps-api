package no.nav.helsearbeidsgiver

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.configureAuth
import no.nav.helsearbeidsgiver.config.configureAuthClient
import no.nav.helsearbeidsgiver.config.configureKafkaConsumers
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.config.configureServices
import no.nav.helsearbeidsgiver.config.configureTolkere
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.plugins.configureRouting
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun main() {
    startServer()
}

fun startServer() {
    val sikkerLogger = sikkerLogger()
    sikkerLogger.info("Setter opp database...")
    val db = DatabaseConfig().init()
    sikkerLogger.info("Setter opp repositories og services...")
    val repositories = configureRepositories(db)

    val authClient = configureAuthClient()

    val services = configureServices(repositories, authClient)
    val tolkere =
        configureTolkere(
            services = services,
            repositories = repositories,
        )

    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            apiModule(services = services, authClient = authClient)
            configureKafkaConsumers(tolkere)
        },
    ).start(wait = true)
}

fun Application.apiModule(
    services: Services,
    authClient: AuthClient,
) {
    val logger = logger()
    logger.info("Starter applikasjon!")
    install(ContentNegotiation) {
        json()
    }
    logger.info("Setter opp autentisering...")
    configureAuth(authClient)

    logger.info("Setter opp routing...")
    configureRouting(services)
}
