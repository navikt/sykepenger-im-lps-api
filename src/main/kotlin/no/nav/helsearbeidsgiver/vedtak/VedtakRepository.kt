package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class VedtakRepository(
    private val db: Database,
) {
    fun lagreVedtak(
        vedtaksperiodeId: UUID,
        fnr: Fnr,
        orgnr: Orgnr,
        vedtak: VedtakArbeidsgiverMelding,
    ) {
        try {
            transaction(db) {
                VedtakEntitet.insert {
                    it[VedtakEntitet.vedtaksperiodeId] = vedtaksperiodeId
                    it[VedtakEntitet.fnr] = fnr.toString()
                    it[VedtakEntitet.orgnr] = orgnr.toString()
                    it[VedtakEntitet.vedtak] = vedtak
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre vedtak med vedtaksperiodeId $vedtaksperiodeId i databasen", e)
            throw e
        }
    }
}
