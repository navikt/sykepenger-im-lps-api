package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.json.jsonb

object SoeknadEntitet : Table("soknad") {
    val id = long("id").autoIncrement()
    val soeknadId = javaUUID("soknad_id")
    val sykmeldingId = javaUUID("sykmelding_id")
    val fnr = varchar("fnr", length = 11)
    val orgnr = varchar("orgnr", length = 9)
    val sykepengesoeknad =
        jsonb<SykepengeSoeknadKafkaMelding>(
            name = "soknad",
            jsonConfig = jsonConfig,
            kSerializer = SykepengeSoeknadKafkaMelding.serializer(),
        )
    val opprettet = datetime("opprettet")
    val vedtaksperiodeId = javaUUID("vedtaksperiode_id").nullable()
}

data class SykepengeSoeknadDto(
    val loepenr: Long,
    val sykepengeSoeknadKafkaMelding: SykepengeSoeknadKafkaMelding,
)
