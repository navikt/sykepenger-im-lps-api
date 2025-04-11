package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb

object SykmeldingEntitet : Table("sykmelding") {
    val id = ulong("id").autoIncrement()
    val sykmeldingId = uuid("sykmelding_id")
    val fnr = varchar("fnr", length = 11)
    val orgnr = varchar("orgnr", length = 9)
    val sendSykmeldingAivenKafkaMessage =
        jsonb<SendSykmeldingAivenKafkaMessage>(
            name = "arbeidsgiver_sykmelding",
            jsonConfig = jsonConfig,
            kSerializer = SendSykmeldingAivenKafkaMessage.serializer(),
        )
    val opprettet = datetime("opprettet")
}
