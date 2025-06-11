package no.nav.helsearbeidsgiver.sis

import no.nav.helsearbeidsgiver.kafka.sis.Behandlingstatusmelding
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class StatusISpeilRepository(
    private val db: Database,
) {
    fun lagreNyeSoeknaderOgStatuser(behandlingstatusmelding: Behandlingstatusmelding) {
        transaction(db) {
            val eksisterendeSoeknader =
                StatusISpeilEntitet
                    .selectAll()
                    .where { StatusISpeilEntitet.vedtaksperiodeId eq behandlingstatusmelding.vedtaksperiodeId }
                    .map { it[StatusISpeilEntitet.soeknadId] }
                    .toSet()
            val nyeSoeknader = behandlingstatusmelding.eksterneSøknadIder.minus(eksisterendeSoeknader)
            nyeSoeknader.forEach { soeknadId ->
                StatusISpeilEntitet.insert {
                    it[StatusISpeilEntitet.soeknadId] = soeknadId
                    it[vedtaksperiodeId] = behandlingstatusmelding.vedtaksperiodeId
                    it[behandlingId] = behandlingstatusmelding.behandlingId
                    it[opprettet] = behandlingstatusmelding.tidspunkt.toLocalDateTime()
                }
            }
        }
    }

    fun hentSoeknaderForVedtaksperiodeId(vedtaksperiodeId: UUID): Set<UUID> =
        transaction(db) {
            StatusISpeilEntitet
                .selectAll()
                .where { StatusISpeilEntitet.vedtaksperiodeId eq vedtaksperiodeId }
                .map { it[StatusISpeilEntitet.soeknadId] }
                .toSet()
        }
}
