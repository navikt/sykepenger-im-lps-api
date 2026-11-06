package no.nav.helsearbeidsgiver.sis

import no.nav.helsearbeidsgiver.kafka.sis.Behandlingstatusmelding
import no.nav.helsearbeidsgiver.utils.log.logger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
