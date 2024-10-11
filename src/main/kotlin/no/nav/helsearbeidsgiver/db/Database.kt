package no.nav.helsearbeidsgiver.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helsearbeidsgiver.Env
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    val dbUser = Env.getProperty("database.username")
    val dbPassword = Env.getProperty("database.password")
    val dbName = Env.getProperty("database.name")
    val host = Env.getProperty("database.host")
    val port = Env.getProperty("database.port")

    val dbUrl = "jdbc:postgresql://%s:%s/%s".format(host, port, dbName)

    fun init() {
        Database.connect(hikari())
        runMigrate()
    }

    private fun runMigrate() {
        val flyway = Flyway.configure().dataSource(dbUrl, dbUser, dbPassword).load()
        flyway.migrate()
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = dbUrl
        config.username = dbUser
        config.password = dbPassword
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}
