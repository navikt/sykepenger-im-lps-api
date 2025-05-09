package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb

object SoknadEntitet : Table("soknad") {
    val id = ulong("id").autoIncrement()
    val soknadId = uuid("soknad_id")
    val sykmeldingId = uuid("sykmelding_id")
    val fnr = varchar("fnr", length = 11)
    val orgnr = varchar("orgnr", length = 9)
    val sykepengesoknad =
        jsonb<SykepengesoknadDTO>(
            name = "soknad",
            jsonConfig = jsonConfig,
            kSerializer = SykepengesoknadDTO.serializer(),
        )
    val opprettet = datetime("opprettet")
}
