package no.nav.helsearbeidsgiver.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helsearbeidsgiver.Env
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource
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
        val dataSource = getDataSource(embedded)
        val database = ExposedDatabase.connect(dataSource) //koble til én gang

        if (embedded) {
            runMigrateEmbedded(dataSource)
        } else {
            runMigrate()
        }
        return database
    }

    private fun getDataSource(embedded: Boolean): DataSource {
        return if (embedded) {
            val config = HikariConfig()
            config.jdbcUrl = "jdbc:h2:mem:test"
            config.username = "root"
            config.driverClassName = "org.h2.Driver"
            config.password = ""
            return HikariDataSource(config)
        } else {
            HikariDataSource(hikari())
        }
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

    private fun runMigrateEmbedded(ds: DataSource) {
        val flyway =
            Flyway
                .configure()
                .sqlMigrationSuffixes("h2")
                .validateMigrationNaming(true)
                .cleanDisabled(false)
                .dataSource(ds)
                .load()
        flyway.clean()
        flyway.migrate()
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
}
