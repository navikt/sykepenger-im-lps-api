package no.nav.helsearbeidsgiver.sis

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.datetime

object StatusISpeilEntitet : Table("status_i_speil") {
    val vedtaksperiodeId = javaUUID("vedtaksperiode_id")
    val soeknadId = javaUUID("soeknad_id")
    val behandlingId = javaUUID("behandling_id")
    val opprettet = datetime("opprettet")
}
