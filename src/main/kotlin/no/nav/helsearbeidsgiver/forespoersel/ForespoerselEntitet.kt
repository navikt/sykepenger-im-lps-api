package no.nav.helsearbeidsgiver.forespoersel

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.datetime

object ForespoerselEntitet : Table("forespoersel") {
    val id = long("id").autoIncrement()
    val navReferanseId = javaUUID(name = "nav_referanse_id")
    val orgnr = text("orgnr")
    val fnr = text("fnr")
    val status = enumerationByName("status", 10, Status::class)
    val opprettet = datetime("opprettet")
    val dokument = text("dokument")
    val eksponertForespoerselId = javaUUID("eksponert_forespoersel_id")
    val vedtaksperiodeId = javaUUID("vedtaksperiode_id")
    val arbeidsgiverperiodePaakrevd = bool("arbeidsgiverperiode_paakrevd")
    val inntektPaakrevd = bool("inntekt_paakrevd")
}
