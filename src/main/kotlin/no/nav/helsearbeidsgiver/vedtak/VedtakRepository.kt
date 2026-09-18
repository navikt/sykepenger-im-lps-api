package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.tilTidspunktEndOfDay
import no.nav.helsearbeidsgiver.utils.tilTidspunktStartOfDay
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.json.extract
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class VedtakRad(
    val loepenr: Long,
    val vedtakId: UUID,
    val fnr: String,
    val orgnr: String,
    val vedtak: VedtakArbeidsgiverMelding,
)

class VedtakRepository(
    private val db: Database,
) {
    fun lagreVedtak(
        vedtakId: UUID,
        vedtaksperiodeId: UUID,
        fnr: Fnr,
        orgnr: Orgnr,
        vedtak: VedtakArbeidsgiverMelding,
    ) {
        try {
            transaction(db) {
                VedtakEntitet.insert {
                    it[VedtakEntitet.vedtakId] = vedtakId
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

    fun hentVedtak(vedtakId: UUID): VedtakRad? =
        transaction(db) {
            VedtakEntitet
                .selectAll()
                .where { VedtakEntitet.vedtakId eq vedtakId }
                .map(::tilVedtakRad)
                .firstOrNull()
        }

    fun hentVedtak(filter: VedtakFilter): List<VedtakRad> =
        transaction(db) {
            val query =
                VedtakEntitet
                    .selectAll()
                    .andWhere { VedtakEntitet.orgnr eq filter.orgnr }
            filter.fnr?.let { query.andWhere { VedtakEntitet.fnr eq it } }
            filter.fom?.let { query.andWhere { VedtakEntitet.opprettet greaterEq it.tilTidspunktStartOfDay() } }
            filter.tom?.let { query.andWhere { VedtakEntitet.opprettet lessEq it.tilTidspunktEndOfDay() } }
            filter.fraLoepenr?.let { query.andWhere { VedtakEntitet.id greater it } }
            filter.vedtakUtfall?.let {
                query.andWhere {
                    VedtakEntitet.vedtak.extract<String>("vedtaksUtfallTilArbeidsgiver") eq it.name
                }
            }
            query.orderBy(VedtakEntitet.id, SortOrder.ASC)
            query.limit(MAX_ANTALL_I_RESPONS + 1)
            query.map(::tilVedtakRad)
        }

    private fun tilVedtakRad(resultatRad: ResultRow) =
        VedtakRad(
            loepenr = resultatRad[VedtakEntitet.id],
            vedtakId = resultatRad[VedtakEntitet.vedtakId],
            fnr = resultatRad[VedtakEntitet.fnr],
            orgnr = resultatRad[VedtakEntitet.orgnr],
            vedtak = resultatRad[VedtakEntitet.vedtak],
        )
}
