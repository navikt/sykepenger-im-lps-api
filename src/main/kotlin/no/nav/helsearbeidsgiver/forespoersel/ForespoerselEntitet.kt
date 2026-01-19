package no.nav.helsearbeidsgiver.forespoersel

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ForespoerselEntitet : Table("forespoersel") {
    val id = long("id").autoIncrement()
    val navReferanseId = uuid(name = "nav_referanse_id")
    val orgnr = text("orgnr")
    val fnr = text("fnr")
    val status = enumerationByName("status", 10, Status::class)
    val opprettet = datetime("opprettet")
    val dokument = text("dokument")
    val eksponertForespoerselId = uuid("eksponert_forespoersel_id")
    val vedtaksperiodeId = uuid("vedtaksperiode_id")
    val arbeidsgiverperiodePaakrevd = bool("arbeidsgiverperiode_paakrevd")
    val inntektPaakrevd = bool("inntekt_paakrevd")
}
