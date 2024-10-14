package no.nav.helsearbeidsgiver.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.inntektsmelding.InnteksMeldingRepositiory
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Database as ExposedDatabase

object Database {
    val dbUser = Env.getProperty("database.username")
    val dbPassword = Env.getProperty("database.password")
    val dbName = Env.getProperty("database.name")
    val host = Env.getProperty("database.host")
    val port = Env.getProperty("database.port")

    val dbUrl = "jdbc:postgresql://%s:%s/%s".format(host, port, dbName)

    fun init() {
        val embedded = Env.getPropertyOrNull("database.embedded").toBoolean()
        if (embedded) {
            val memDatabase =
                Database.connect(
                    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                    user = "root",
                    driver = "org.h2.Driver",
                    password = "",
                )
            InnteksMeldingRepositiory(memDatabase)
        } else {
            val pgDatabase = ExposedDatabase.connect(hikari())
            InnteksMeldingRepositiory(pgDatabase)
        }

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
}
