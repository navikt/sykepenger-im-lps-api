package no.nav.no.nav.helsearbeidsgiver

import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals

@Testcontainers
class ITest {

    companion object {
        @Container
        val postgresContainer =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")
    }

    @Test
    fun `should connect to PostgreSQL and execute query`() {
        val jdbcUrl = postgresContainer.jdbcUrl
        val username = postgresContainer.username
        val password = postgresContainer.password

        val connection: Connection = DriverManager.getConnection(jdbcUrl, username, password)

        connection.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL
            )
            """.trimIndent(),
        )

        connection.createStatement().execute("INSERT INTO users (name) VALUES ('John Doe')")

        val resultSet = connection.createStatement().executeQuery("SELECT COUNT(*) FROM users")
        resultSet.next()
        val count = resultSet.getInt(1)

        assertEquals(1, count)

        connection.close()
    }
}
