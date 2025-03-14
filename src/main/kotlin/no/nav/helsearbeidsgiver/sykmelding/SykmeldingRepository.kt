package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.arbeidsgiverSykmelding
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class SykmeldingRepository(
    private val db: Database,
) {
    private val sikkerLogger = sikkerLogger()

    fun opprettSykmelding(sykmeldingMessage: SendSykmeldingAivenKafkaMessage): UUID {
        sikkerLogger.info("Lagrer sykmelding i database.")
        val dokument =
            transaction(db) {
                SykmeldingEntitet.insert {
                    it[sykmeldingId] = UUID.fromString(sykmeldingMessage.sykmelding.id)
                    it[fnr] = sykmeldingMessage.kafkaMetadata.fnr
                    it[arbeidsgiverSykmelding] = sykmeldingMessage.sykmelding
                }
            }
        return dokument[SykmeldingEntitet.sykmeldingId]
    }

    fun hentSykmeldingForSykmeldingId(sykmeldingId: UUID): ArbeidsgiverSykmelding? =
        transaction(db) {
            SykmeldingEntitet
                .selectAll()
                .where { SykmeldingEntitet.sykmeldingId eq sykmeldingId }
                .firstOrNull()
                ?.getOrNull(arbeidsgiverSykmelding)
        }
}
