package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.mottattAvNav
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.opprettet
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.orgnr
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sykmeldingId
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sykmeldtNavn
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.tilTidspunktEndOfDay
import no.nav.helsearbeidsgiver.utils.tilTidspunktStartOfDay
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class SykmeldingRepository(
    private val db: Database,
) {
    fun lagreSykmelding(
        sykmeldingId: UUID,
        fnr: String,
        orgnr: String,
        sykmelding: SendSykmeldingAivenKafkaMessage,
        sykmeldtNavn: String,
        opprettet: LocalDateTime = LocalDateTime.now(), // Kan overrides fra tester
    ) {
        try {
            transaction(db) {
                SykmeldingEntitet.insert {
                    it[this.sykmeldingId] = sykmeldingId
                    it[SykmeldingEntitet.fnr] = fnr
                    it[SykmeldingEntitet.orgnr] = orgnr
                    it[SykmeldingEntitet.sykmeldtNavn] = sykmeldtNavn
                    it[sendSykmeldingAivenKafkaMessage] = sykmelding
                    it[mottattAvNav] = sykmelding.sykmelding.mottattTidspunkt.toLocalDateTime()
                    it[SykmeldingEntitet.opprettet] = opprettet
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre sykmelding $sykmeldingId i database: ${e.message}")
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

    fun hentSykmeldinger(filter: SykmeldingFilter): List<SykmeldingDTO> =
        transaction(db) {
            val query =
                SykmeldingEntitet
                    .selectAll()
                    .where { orgnr eq filter.orgnr }
            filter.fnr?.let { query.andWhere { fnr eq it } }
            filter.fom?.let { query.andWhere { opprettet greaterEq it.tilTidspunktStartOfDay() } }
            filter.tom?.let { query.andWhere { opprettet lessEq it.tilTidspunktEndOfDay() } }
            filter.fraLoepenr?.let { query.andWhere { SykmeldingEntitet.id greater it } }
            query.orderBy(SykmeldingEntitet.id, SortOrder.ASC)
            query.limit(MAX_ANTALL_I_RESPONS + 1) // Legg på en, for å kunne sjekke om det faktisk finnes flere enn max antall
            query
                .map { it.toSykmelding() }
        }

    private fun ResultRow.toSykmelding(): SykmeldingDTO =
        SykmeldingDTO(
            loepenr = this[SykmeldingEntitet.id],
            id = this[sykmeldingId].toString(),
            orgnr = this[orgnr],
            fnr = this[fnr],
            sendSykmeldingAivenKafkaMessage = this[sendSykmeldingAivenKafkaMessage],
            sykmeldtNavn = this[sykmeldtNavn],
            mottattAvNav = this[mottattAvNav],
            sendtTilArbeidsgiver = this[opprettet],
        )
}
