package no.nav.helsearbeidsgiver.refusjon

import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.json.jsonb

object RefusjonUtfallEntitet : Table("refusjon_utfall") {
    val id = long("id").autoIncrement()
    val refusjonUtfallId = javaUUID("refusjon_utfall_id")
    val vedtaksperiodeId = javaUUID("vedtaksperiode_id")
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

// Henter fra database uten å parse json-payload
object RefusjonUtfallMetadataEntitet : Table("refusjon_utfall") {
    val refusjonUtfallId = javaUUID("refusjon_utfall_id")
    val vedtaksperiodeId = javaUUID("vedtaksperiode_id")
    val fnr = text("fnr")
    val orgnr = text("orgnr")
    val opprettet = datetime("opprettet")
}
