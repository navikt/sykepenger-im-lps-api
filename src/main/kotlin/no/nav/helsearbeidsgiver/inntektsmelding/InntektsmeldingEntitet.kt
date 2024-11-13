package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object InntektsmeldingEntitet : Table("inntektsmelding") {
    val id = integer("id").autoIncrement()
    val dokument = json<Inntektsmelding>("dokument", jsonMapper)
    val orgnr = varchar("orgnr", length = 9)
    val fnr = varchar("fnr", length = 11)
    val foresporselid = varchar("foresporselid", length = 40).nullable()
    val innsendt = datetime("innsendt").default(LocalDateTime.now())
    val mottattEvent = datetime("mottatt_event").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(id)
}
