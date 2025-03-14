package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.log.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class SykmeldingRepository(
    private val db: Database,
) {
    fun lagreSykmelding(sykmeldingMessage: SendSykmeldingAivenKafkaMessage): UUID {
        logger().info("Lagrer sykmelding med id ${sykmeldingMessage.sykmelding.id}.")
        val orgnummer =
            sykmeldingMessage.event.arbeidsgiver?.orgnummer
                ?: logAndThrowSykmeldingOrgnrManglerException(sykmeldingMessage.sykmelding.id)
        val dokument =
            transaction(db) {
                SykmeldingEntitet.insert {
                    it[sykmeldingId] = UUID.fromString(sykmeldingMessage.sykmelding.id)
                    it[fnr] = sykmeldingMessage.kafkaMetadata.fnr
                    it[orgnr] = orgnummer
                    it[arbeidsgiverSykmelding] = sykmeldingMessage.sykmelding
                }
            }
        return dokument[SykmeldingEntitet.sykmeldingId]
    }

    private fun logAndThrowSykmeldingOrgnrManglerException(sykmeldingId: String): Nothing {
        "Sykmelding med sykmeldingId $sykmeldingId ble ikke lagret fordi den mangler orgnr.".also {
            logger().error(it)
            throw SykmeldingOrgnrManglerException(it)
        }
    }
}
