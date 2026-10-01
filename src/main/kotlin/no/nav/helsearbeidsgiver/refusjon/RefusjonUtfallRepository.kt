package no.nav.helsearbeidsgiver.refusjon

import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.tilTidspunktEndOfDay
import no.nav.helsearbeidsgiver.utils.tilTidspunktStartOfDay
import no.nav.helsearbeidsgiver.vedtak.VedtakFilter
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
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

    fun hentRefusjonUtfall(filter: VedtakFilter): List<RefusjonUtfallRad> =
        transaction(db) {
            val query =
                RefusjonUtfallEntitet
                    .selectAll()
                    .andWhere { RefusjonUtfallEntitet.orgnr eq filter.orgnr }
            filter.fnr?.let { query.andWhere { RefusjonUtfallEntitet.fnr eq it } }
            filter.fom?.let { query.andWhere { RefusjonUtfallEntitet.opprettet greaterEq it.tilTidspunktStartOfDay() } }
            filter.tom?.let { query.andWhere { RefusjonUtfallEntitet.opprettet lessEq it.tilTidspunktEndOfDay() } }
            filter.fraLoepenr?.let { query.andWhere { RefusjonUtfallEntitet.id greater it } }
            query.orderBy(RefusjonUtfallEntitet.id, SortOrder.ASC)
            query.limit(MAX_ANTALL_I_RESPONS + 1)
            query.map(::tilRefusjonUtfallRad)
        }

    private fun tilRefusjonUtfallRad(resultatRad: ResultRow) =
        RefusjonUtfallRad(
            loepenr = resultatRad[RefusjonUtfallEntitet.id],
            refusjonUtfall = resultatRad[RefusjonUtfallEntitet.refusjonUtfall],
        )
}
