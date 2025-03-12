package no.nav.helsearbeidsgiver.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helsearbeidsgiver.Env
import org.flywaydb.core.Flyway
import javax.sql.DataSource
import org.jetbrains.exposed.sql.Database as ExposedDatabase

object Database {
    private val dbUser = Env.getProperty("database.username")
    private val dbPassword = Env.getProperty("database.password")
    private val dbName = Env.getProperty("database.name")
    private val host = Env.getProperty("database.host")
    private val port = Env.getProperty("database.port")

    private val jdbcUrl = Env.getPropertyOrNull("database.url") ?: "jdbc:postgresql://%s:%s/%s".format(host, port, dbName)
    private val embedded = Env.getPropertyOrNull("database.embedded").toBoolean()

    fun init(): ExposedDatabase {
        val dataSource = getDataSource()
        runMigrate(dataSource)
        return ExposedDatabase.connect(dataSource)
    }

    private fun getDataSource(): DataSource =
        if (embedded) {
            embeddedH2()
        } else {
            postgres()
        }

    private fun runMigrate(dataSource: DataSource) {
        if (embedded) {
            val flyway =
                Flyway
                    .configure()
                    .sqlMigrationSuffixes("h2")
                    .validateMigrationNaming(true)
                    .cleanDisabled(false)
                    .dataSource(dataSource)
                    .load()
            flyway.clean()
            flyway.migrate()
        } else {
            val flyway =
                Flyway
                    .configure()
                    .validateMigrationNaming(true)
                    .dataSource(dataSource)
                    .load()
            flyway.migrate()
            flyway.validate()
        }
    }

    private fun postgres(): HikariDataSource {
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

    private fun embeddedH2(): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.username = "root"
        config.driverClassName = "org.h2.Driver"
        config.password = ""
        return HikariDataSource(config)
    }
}
