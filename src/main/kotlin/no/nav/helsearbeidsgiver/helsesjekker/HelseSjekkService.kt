package no.nav.helsearbeidsgiver.helsesjekker

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class HelseSjekkService(
    private val db: Database,
) {
    fun databaseIsAlive(): Boolean =
        try {
            transaction(db) {
                exec("SELECT 1") { rs -> rs.next() }
            } ?: false
        } catch (e: Exception) {
            sikkerLogger().error(e.message, e)
            false
        }
}
