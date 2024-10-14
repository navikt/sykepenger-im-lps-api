package no.nav.helsearbeidsgiver.inntektsmelding

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

data class ExposedInntektsmelding(
    val inntektsMelding: String,
)

class InnteksMeldingRepositiory(
    database: Database,
) {
    object InntektsMelding : Table() {
        val id = integer("id").autoIncrement()
        val inntektsMelding = text("inntektsMelding")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(InntektsMelding)
        }
    }
}
