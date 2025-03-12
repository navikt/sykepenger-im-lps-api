package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb

object SykmeldingEntitet : Table("sykmelding") {
    val id =
        ulong("id").autoIncrement(
            idSeqName = "sykmelding_id_seq",
        )
    val sykmeldingId = uuid("sykmelding_id")
    val fnr = varchar("fnr", length = 11)
    val sykmelding =
        jsonb<ArbeidsgiverSykmelding>(
            name = "inntektsmelding",
            jsonConfig = jsonConfig,
            kSerializer = ArbeidsgiverSykmelding.serializer(),
        )
    val opprettet = datetime("opprettet")
}
