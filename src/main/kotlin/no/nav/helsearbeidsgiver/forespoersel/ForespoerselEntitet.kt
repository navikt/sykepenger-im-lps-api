package no.nav.helsearbeidsgiver.forespoersel

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ForespoerselEntitet : Table("forespoersel") {
    val forespoersel = varchar(name = "forespoersel_id", length = 40)
    val orgnr = text("orgnr")
    val fnr = text("fnr")
    val status = text("status")
    val opprettet = datetime("opprettet")
}
