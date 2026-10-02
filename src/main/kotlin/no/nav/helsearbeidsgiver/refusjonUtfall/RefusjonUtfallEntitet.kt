package no.nav.helsearbeidsgiver.refusjonUtfall

import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb

object RefusjonUtfallEntitet : Table("refusjon_utfall") {
    val id = long("id").autoIncrement()
    val refusjonUtfallId = uuid("refusjon_utfall_id")
    val vedtaksperiodeId = uuid("vedtaksperiode_id")
    val fnr = text("fnr")
    val orgnr = text("orgnr")
    val refusjonUtfall =
        jsonb<RefusjonUtfall>(
            name = "refusjon_utfall",
            jsonConfig = jsonConfig,
            kSerializer = RefusjonUtfall.serializer(),
        )
    val opprettet = datetime("opprettet")
}
