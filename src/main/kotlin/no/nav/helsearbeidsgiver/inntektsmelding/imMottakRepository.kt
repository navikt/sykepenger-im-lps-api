package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database.dbQuery
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert

data class ExposedMottak(
    val inntektsMelding: String,
)

class ImMottakRepository {
    object MottakTable : Table() {
        val id = integer("id").autoIncrement()
        val melding = text("melding")

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun opprett(im: ExposedMottak): Int =
        dbQuery {
            MottakTable.insert {
                it[melding] = im.inntektsMelding
            }[MottakTable.id]
        }
}
