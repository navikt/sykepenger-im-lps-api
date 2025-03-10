package no.nav.helsearbeidsgiver.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helsearbeidsgiver.Env
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Database as ExposedDatabase

object Database {
    val dbUser = Env.getProperty("database.username")
    val dbPassword = Env.getProperty("database.password")
    val dbName = Env.getProperty("database.name")
    val host = Env.getProperty("database.host")
    val port = Env.getProperty("database.port")

    val jdbcUrl = Env.getPropertyOrNull("database.url") ?: "jdbc:postgresql://%s:%s/%s".format(host, port, dbName)

    fun init(): Database {
        val embedded = Env.getPropertyOrNull("database.embedded").toBoolean()
        val db = getDatabase(embedded)

        if (embedded) {
            runMigrateEmbedded()
        } else {
            runMigrate()
        }
        return db
    }

    private fun getDatabase(embedded: Boolean): Database =
        if (embedded) {
            ExposedDatabase.connect(hikariH2())
        } else {
            ExposedDatabase.connect(hikari())
        }

    private fun runMigrate() {
        val flyway =
            Flyway
                .configure()
                .validateMigrationNaming(true)
                .dataSource(jdbcUrl, dbUser, dbPassword)
                .load()
        flyway.migrate()
        flyway.validate()
    }

    private fun runMigrateEmbedded() {
        val flyway =
            Flyway
                .configure()
                .sqlMigrationSuffixes("h2")
                .validateMigrationNaming(true)
                .dataSource("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
                .load()
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
        config.isAutoCommit = true
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    private fun hikariH2(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
        config.username = "root"
        config.password = ""
        config.maximumPoolSize = 3
        config.isAutoCommit = true
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    fun getDataSource(): HikariDataSource =
        if (Env.getPropertyOrNull("database.embedded").toBoolean()) {
            hikariH2()
        } else {
            hikari()
        }
}
