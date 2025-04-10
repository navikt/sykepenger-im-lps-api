package no.nav.helsearbeidsgiver.testcontainer

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(PostgresTestExtension::class)
annotation class WithPostgresContainer

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(KafkaTestExtension::class)
annotation class WithKafkaContainer

class PostgresTestExtension :
    BeforeAllCallback,
    AfterAllCallback {
    companion object {
        private val postgresContainer: PostgreSQLContainer<*> by lazy {
            withRetries(
                feilmelding = "Klarte ikke sette opp inntektsmeldingDatabase.",
            ) {
                PostgreSQLContainer("postgres:16")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .waitingFor(Wait.forListeningPort())
                    .withStartupTimeout(Duration.ofSeconds(60))
            }
        }
    }

    override fun beforeAll(context: ExtensionContext) {
        postgresContainer.start()
        System.setProperty("database.url", postgresContainer.jdbcUrl)
        System.setProperty("database.username", postgresContainer.username)
        System.setProperty("database.password", postgresContainer.password)
    }

    override fun afterAll(context: ExtensionContext) {
        postgresContainer.stop()
    }
}

class KafkaTestExtension :
    BeforeAllCallback,
    AfterAllCallback {
    companion object {
        private val kafkaContainer = ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
    }

    override fun beforeAll(context: ExtensionContext) {
        kafkaContainer.start()
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", kafkaContainer.bootstrapServers)
    }

    override fun afterAll(context: ExtensionContext) {
        kafkaContainer.stop()
    }
}

private fun <T> withRetries(
    antallForsoek: Int = 5,
    pauseMillis: Long = 1000,
    feilmelding: String,
    blokk: () -> T,
): T {
    repeat(antallForsoek) {
        runCatching { blokk() }
            .onSuccess { return it }
            .onFailure { runBlocking { delay(pauseMillis) } }
    }
    throw IllegalStateException(feilmelding)
}
