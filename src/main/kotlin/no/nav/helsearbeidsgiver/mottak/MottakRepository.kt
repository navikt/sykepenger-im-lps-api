package no.nav.helsearbeidsgiver.mottak

import no.nav.helsearbeidsgiver.mottak.MottakRepository.MottakTable.melding
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class ExposedMottak(
    val melding: String,
    val gyldig: Boolean = true,
)

class MottakRepository(
    private val db: Database,
) {
    object MottakTable : Table() {
        val id = integer("id").autoIncrement()
        val melding = text("melding")
        val gyldig = bool("gyldig")
        val opprettet = datetime("opprettet")
        override val primaryKey = PrimaryKey(id)
    }

    fun opprett(im: ExposedMottak): Int =
        transaction(db) {
            MottakTable.insert {
                it[melding] = im.melding
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
