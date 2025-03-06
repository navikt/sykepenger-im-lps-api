package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object InnsendingEntitet : Table("innsending") {
    val id = uuid("id").autoGenerate()
    val dokument = json<SkjemaInntektsmelding>("dokument", jsonMapper)
    val orgnr = varchar("orgnr", length = 9)
    val fnr = varchar("fnr", length = 11)
    val lps = varchar("lps", length = 40)
    val foresporselid = uuid("foresporsel_id")
    val innsendtdato = datetime("innsendt_dato").default(LocalDateTime.now())
    val inntektsmeldingid = integer("inntektsmelding_id").references(InntektsmeldingEntitet.id).nullable()
    val status = enumerationByName("status", 15, InnsendingStatus::class)
    val feilaarsak = varchar("feil_aarsak", length = 500).nullable()
    override val primaryKey = PrimaryKey(id)
}
