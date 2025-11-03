package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb

object SoeknadEntitet : Table("soknad") {
    val id = long("id").autoIncrement()
    val soeknadId = uuid("soknad_id")
    val sykmeldingId = uuid("sykmelding_id")
    val fnr = varchar("fnr", length = 11)
    val orgnr = varchar("orgnr", length = 9)
    val sykepengesoeknad =
        jsonb<SykepengeSoeknadKafkaMelding>(
            name = "soknad",
            jsonConfig = jsonConfig,
            kSerializer = SykepengeSoeknadKafkaMelding.serializer(),
        )
    val opprettet = datetime("opprettet")
    val vedtaksperiodeId = uuid("vedtaksperiode_id").nullable()
}

data class SykepengeSoeknadDto(
    val loepenr: Long,
    val sykepengeSoeknadKafkaMelding: SykepengeSoeknadKafkaMelding,
)
