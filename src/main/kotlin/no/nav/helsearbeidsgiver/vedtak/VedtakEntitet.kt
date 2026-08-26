package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb

object VedtakEntitet : Table("vedtak") {
    val id = long("id").autoIncrement()
    val vedtaksperiodeId = uuid("vedtaksperiode_id")
    val fnr = varchar("fnr", length = 11)
    val orgnr = varchar("orgnr", length = 9)
    val vedtak =
        jsonb<VedtakArbeidsgiverMelding>(
            name = "vedtak",
            jsonConfig = jsonConfig,
            kSerializer = VedtakArbeidsgiverMelding.serializer(),
        )
    val opprettet = datetime("opprettet")
}
