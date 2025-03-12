package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
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
                    it[sykmeldingId] = sykmeldingMessage.sykmelding.id
                    it[fnr] = sykmeldingMessage.kafkaMetadata.fnr
                    it[sykmelding] = sykmelding
                }
            }
        return dokument[SykmeldingEntitet.sykmeldingId]
    }
}
