package no.nav.helsearbeidsgiver.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.Database as ExposedDatabase

object Database {
    val dbUser = Env.getProperty("database.username")
    val dbPassword = Env.getProperty("database.password")
    val dbName = Env.getProperty("database.name")
    val host = Env.getProperty("database.host")
    val port = Env.getProperty("database.port")

    val jdbcUrl = Env.getPropertyOrNull("database.url") ?: "jdbc:postgresql://%s:%s/%s".format(host, port, dbName)

    fun init() {
        val embedded = Env.getPropertyOrNull("database.embedded").toBoolean()
        val db = getDatabase(embedded)
        if (!embedded) {
            runMigrate()
        }
        InntektsMeldingRepository()
    }

    private fun getDatabase(embedded: Boolean): Database =
        if (embedded) {
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                user = "root",
                driver = "org.h2.Driver",
                password = "",
            )
        } else {
            ExposedDatabase.connect(hikari())
        }

    private fun runMigrate() {
        val flyway = Flyway.configure().validateMigrationNaming(true).dataSource(jdbcUrl, dbUser, dbPassword).load()
        flyway.migrate()
        flyway.validate()
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = jdbcUrl
        config.username = dbUser
        config.password = dbPassword
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }
}
