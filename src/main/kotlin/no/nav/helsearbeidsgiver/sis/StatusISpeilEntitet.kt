package no.nav.helsearbeidsgiver.sis

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object StatusISpeilEntitet : Table("status_i_speil") {
    val vedtaksperiodeId = uuid("vedtaksperiode_id")
    val soeknadId = uuid("soeknad_id")
    val behandlingId = uuid("behandling_id")
    val opprettet = datetime("opprettet")
}
