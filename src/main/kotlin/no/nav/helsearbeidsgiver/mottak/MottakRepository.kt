package no.nav.helsearbeidsgiver.mottak

import no.nav.helsearbeidsgiver.mottak.MottakRepository.MottakTable.melding
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class ExposedMottak(
    val inntektsMelding: String,
    val gyldig: Boolean = true,
)

class MottakRepository(
    private val db: Database,
) {
    object MottakTable : Table() {
        val id = integer("id").autoIncrement()
        val melding = text("melding")
        val gyldig = bool("gyldig")
        override val primaryKey = PrimaryKey(id)
    }

    fun opprett(im: ExposedMottak): Int =
        transaction(db) {
            MottakTable.insert {
                it[melding] = im.inntektsMelding
                it[gyldig] = im.gyldig
            }[MottakTable.id]
        }

    fun hent(id: Int): ExposedMottak? =
        transaction(db) {
            MottakTable
                .selectAll()
                .where { MottakTable.id eq id }
                .map {
                    ExposedMottak(it[melding])
                }.firstOrNull()
        }
}
