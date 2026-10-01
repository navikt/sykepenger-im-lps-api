package no.nav.helsearbeidsgiver.refusjon

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class RefusjonUtfallRad(
    val loepenr: Long,
    val refusjonUtfall: RefusjonUtfall,
)

class RefusjonUtfallRepository(
    private val db: Database,
) {
    fun lagreRefusjonUtfall(refusjonUtfall: RefusjonUtfall) {
        try {
            transaction(db) {
                RefusjonUtfallEntitet.insert {
                    it[RefusjonUtfallEntitet.refusjonUtfallId] = refusjonUtfall.refusjonUtfallId
                    it[RefusjonUtfallEntitet.vedtaksperiodeId] = refusjonUtfall.vedtaksperiodeId
                    it[RefusjonUtfallEntitet.fnr] = refusjonUtfall.fnr.toString()
                    it[RefusjonUtfallEntitet.orgnr] = refusjonUtfall.orgnr.toString()
                    it[RefusjonUtfallEntitet.refusjonUtfall] = refusjonUtfall
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error(
                "Klarte ikke å lagre refusjonsutfall med refusjonUtfallId ${refusjonUtfall.refusjonUtfallId} " +
                    "og vedtaksperiodeId ${refusjonUtfall.vedtaksperiodeId} i databasen",
                e,
            )
            throw e
        }
    }

    fun hentRefusjonUtfall(refusjonUtfallId: UUID): RefusjonUtfallRad? =
        transaction(db) {
            RefusjonUtfallEntitet
                .selectAll()
                .where { RefusjonUtfallEntitet.refusjonUtfallId eq refusjonUtfallId }
                .map(::tilRefusjonUtfallRad)
                .firstOrNull()
        }

    private fun tilRefusjonUtfallRad(resultatRad: ResultRow) =
        RefusjonUtfallRad(
            loepenr = resultatRad[RefusjonUtfallEntitet.id],
            refusjonUtfall = resultatRad[RefusjonUtfallEntitet.refusjonUtfall],
        )
}
