package no.nav.helsearbeidsgiver.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helsearbeidsgiver.Env
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database as ExposedDatabase

class DatabaseConfig(
    val jdbcUrl: String = Env.getProperty("database.url"),
    val username: String = Env.getProperty("database.username"),
    val password: String = Env.getProperty("database.password"),
) {
    fun init(): ExposedDatabase {
        val dataSource = postgresDataSource()
        runMigrate(dataSource)
        return ExposedDatabase.connect(dataSource)
    }

    private fun postgresDataSource(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = jdbcUrl
        config.username = username
        config.password = password
        config.maximumPoolSize = 3
        config.isAutoCommit = true
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    private fun runMigrate(dataSource: HikariDataSource) {
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
