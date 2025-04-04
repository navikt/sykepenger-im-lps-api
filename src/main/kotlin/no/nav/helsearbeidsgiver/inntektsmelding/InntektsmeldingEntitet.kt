package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object InntektsmeldingEntitet : Table("inntektsmelding") {
    val id = integer("id").autoIncrement() // TODO: bytte navn til serienr?
    val innsendingId = uuid("innsending_id") // TODO: kalle denne for id..?
    val dokument = json<Inntektsmelding>("dokument", jsonMapper)
    val orgnr = varchar("orgnr", length = 9)
    val fnr = varchar("fnr", length = 11)
    val innsendt = datetime("innsendt").default(LocalDateTime.now())
    val skjema = json<SkjemaInntektsmelding>("skjema", jsonMapper)
    val aarsakInnsending = enumerationByName("aarsak_innsending", length = 7, AarsakInnsending::class)
    val typeInnsending = enumerationByName("type_innsending", length = 21, InnsendingType::class)
    val avsenderSystemNavn = varchar("avsender_system_navn", length = 32)
    val avsenderSystemVersjon = varchar("avsender_system_versjon", length = 10)
    val navReferanseId = uuid("nav_referanse_id")
    val versjon = integer("versjon")
    val status = enumerationByName("status", 15, InnsendingStatus::class)
    val statusMelding = varchar("status_melding", length = 255).nullable()
    override val primaryKey = PrimaryKey(id)
}
