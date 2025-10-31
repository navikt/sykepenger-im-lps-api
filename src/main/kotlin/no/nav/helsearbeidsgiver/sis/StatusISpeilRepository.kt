package no.nav.helsearbeidsgiver.sis

import no.nav.helsearbeidsgiver.kafka.sis.Behandlingstatusmelding
import no.nav.helsearbeidsgiver.utils.log.logger
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
            val eksisterendeSoeknadIder =
                StatusISpeilEntitet
                    .selectAll()
                    .where { StatusISpeilEntitet.vedtaksperiodeId eq behandlingstatusmelding.vedtaksperiodeId }
                    .map { it[StatusISpeilEntitet.soeknadId] }
                    .toSet()
            val nyeSoeknadIder = behandlingstatusmelding.eksterneSøknadIder?.minus(eksisterendeSoeknadIder)
            nyeSoeknadIder?.forEach { soeknadId ->
                logger().info("Lagrer soeknad $soeknadId på vedtaksperiode ${behandlingstatusmelding.vedtaksperiodeId}")
                StatusISpeilEntitet.insert {
                    it[StatusISpeilEntitet.soeknadId] = soeknadId
                    it[vedtaksperiodeId] = behandlingstatusmelding.vedtaksperiodeId
                    it[behandlingId] = behandlingstatusmelding.behandlingId
                    it[opprettet] = behandlingstatusmelding.tidspunkt.toLocalDateTime()
                }
            }
        }
    }

    fun hentSoeknadIderForVedtaksperiodeId(vedtaksperiodeId: UUID): List<UUID> =
        transaction(db) {
            StatusISpeilEntitet
                .selectAll()
                .where { StatusISpeilEntitet.vedtaksperiodeId eq vedtaksperiodeId }
                .map { it[StatusISpeilEntitet.soeknadId] }
        }
}
