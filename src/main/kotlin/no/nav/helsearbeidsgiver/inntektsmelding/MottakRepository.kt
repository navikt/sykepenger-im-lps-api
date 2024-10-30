package no.nav.helsearbeidsgiver.inntektsmelding

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

data class ExposedMottak(
    val inntektsMelding: String,
)

class MottakRepository(
    private val db: Database,
) {
    object MottakTable : Table() {
        val id = integer("id").autoIncrement()
        val melding = text("melding")
        override val primaryKey = PrimaryKey(id)
    }

    fun opprett(im: ExposedMottak): Int =
        transaction(db) {
            MottakTable.insert {
                it[melding] = im.inntektsMelding
            }[MottakTable.id]
        }
}
