package no.nav.helsearbeidsgiver.forespoersel

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ForespoerselEntitet : Table("forespoersel") {
    val navReferanseId = uuid(name = "nav_referanse_id")
    val orgnr = text("orgnr")
    val fnr = text("fnr")
    val status = enumerationByName("status", 10, Status::class)
    val opprettet = datetime("opprettet")
    val dokument = text("dokument")
    val eksponertForespoerselId = uuid("eksponert_forespoersel_id").nullable()
    val vedtaksperiodeId = uuid("vedtaksperiode_id").nullable()
}
