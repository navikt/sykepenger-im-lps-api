package no.nav.helsearbeidsgiver.helsesjekker

import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

interface HelseSjekkService {
    fun isReady(): Boolean
}

class DatabaseHelseSjekkService(
    val db: Database,
) : HelseSjekkService {
    override fun isReady(): Boolean =
        try {
            transaction(db) {
                exec("SELECT 1") { rs -> rs.next() }
            } ?: false
        } catch (e: Exception) {
            logger().error("Helsesjekk mot database feilet")
            sikkerLogger().error("Helsesjekk mot database feilet", e)
            false
        }
}
