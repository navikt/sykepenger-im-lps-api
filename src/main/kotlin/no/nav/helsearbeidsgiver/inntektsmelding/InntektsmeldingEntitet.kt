package no.nav.helsearbeidsgiver.inntektsmelding

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object InntektsmeldingEntitet : Table("inntektsmelding") {
    val id = integer("id").autoIncrement()
    val dokument = text("dokument")
    val orgnr = varchar("orgnr", length = 9)
    val fnr = varchar("fnr", length = 11)
    val foresporselid = varchar("foresporselid", length = 40)
    val innsendt = datetime("innsendt")
    val mottattEvent = datetime("mottatt_event")
    override val primaryKey = PrimaryKey(id)
}
