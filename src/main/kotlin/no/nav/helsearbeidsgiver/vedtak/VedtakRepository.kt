package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class VedtakRepository(
    private val db: Database,
) {
    fun lagreVedtak(vedtak: LagreVedtak) {
        try {
            transaction(db) {
                VedtakEntitet.insert {
                    it[vedtaksperiodeId] = vedtak.vedtaksperiodeId
                    it[fnr] = vedtak.fnr
                    it[orgnr] = vedtak.orgnr
                    it[VedtakEntitet.vedtak] = vedtak.vedtak
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre vedtak med vedtaksperiodeId ${vedtak.vedtaksperiodeId} i databasen", e)
            throw e
        }
    }
}
