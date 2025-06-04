package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.opprettet
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.orgnr
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sykmeldingId
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sykmeldtNavn
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class SykmeldingRepository(
    private val db: Database,
) {
    fun lagreSykmelding(
        id: UUID,
        fnr: String,
        orgnr: String,
        sykmelding: SendSykmeldingAivenKafkaMessage,
        sykmeldtNavn: String,
        opprettet: LocalDateTime = LocalDateTime.now(), // Skal kun overstyres ifbm testing
    ) {
        try {
            transaction(db) {
                SykmeldingEntitet.insert {
                    it[sykmeldingId] = id
                    it[SykmeldingEntitet.fnr] = fnr
                    it[SykmeldingEntitet.orgnr] = orgnr
                    it[SykmeldingEntitet.sykmeldtNavn] = sykmeldtNavn
                    it[sendSykmeldingAivenKafkaMessage] = sykmelding
                    it[SykmeldingEntitet.opprettet] = opprettet
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre sykmelding $id i database: ${e.message}")
            throw e
        }
    }

    fun hentSykmelding(id: UUID): SykmeldingDTO? =
        transaction(db) {
            SykmeldingEntitet
                .selectAll()
                .where { sykmeldingId eq id }
                .map { it.toSykmelding() }
                .firstOrNull()
        }

    private fun ResultRow.toSykmelding(): SykmeldingDTO =
        SykmeldingDTO(
            id = this[sykmeldingId].toString(),
            orgnr = this[orgnr],
            fnr = this[fnr],
            sendSykmeldingAivenKafkaMessage = this[sendSykmeldingAivenKafkaMessage],
            sykmeldtNavn = this[sykmeldtNavn],
            opprettet = this[opprettet],
        )

    fun hentSykmeldinger(
        orgnr: String,
        filter: SykmeldingFilterRequest? = null,
    ): List<SykmeldingDTO> =
        transaction(db) {
            SykmeldingEntitet
                .selectAll()
                .where {
                    listOfNotNull(
                        SykmeldingEntitet.orgnr eq orgnr,
                        filter?.fnr?.let { fnr eq it },
                        filter?.fom?.let { opprettet greaterEq LocalDateTime.of(it.year, it.month, it.dayOfMonth, 0, 0) },
                        filter?.tom?.let { opprettet lessEq LocalDateTime.of(it.year, it.month, it.dayOfMonth, 0, 0) },
                    ).reduce { acc, cond -> acc and cond }
                }.map { it.toSykmelding() }
        }
}
