package no.nav.helsearbeidsgiver.forespoersel

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ForespoerselEntitet : Table("forespoersel") {
    val forespoerselId = uuid(name = "forespoersel_id")
    val orgnr = text("orgnr")
    val fnr = text("fnr")
    val status = enumerationByName("status", 10, Status::class)
    val opprettet = datetime("opprettet")
    val dokument = text("dokument")
}
