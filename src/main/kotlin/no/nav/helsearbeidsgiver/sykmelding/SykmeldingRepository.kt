package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.mottattAvNav
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
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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
    ) {
        try {
            transaction(db) {
                SykmeldingEntitet.insert {
                    it[sykmeldingId] = id
                    it[SykmeldingEntitet.fnr] = fnr
                    it[SykmeldingEntitet.orgnr] = orgnr
                    it[SykmeldingEntitet.sykmeldtNavn] = sykmeldtNavn
                    it[sendSykmeldingAivenKafkaMessage] = sykmelding
                    it[mottattAvNav] = sykmelding.sykmelding.mottattTidspunkt.toLocalDateTime()
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre sykmelding $id i database: ${e.message}")
            throw e
        }
    }

    fun hentSykmelding(id: UUID): SykmeldingResponse? =
        transaction(db) {
            SykmeldingEntitet
                .selectAll()
                .where { sykmeldingId eq id }
                .map { it.toSykmelding() }
                .firstOrNull()
        }

    fun hentSykmeldinger(filter: SykmeldingFilter): List<SykmeldingResponse> =
        transaction(db) {
            val query =
                SykmeldingEntitet
                    .selectAll()
                    .where { orgnr eq filter.orgnr }
            filter.fnr?.let { query.andWhere { fnr eq it } }
            filter.fom?.let { query.andWhere { mottattAvNav greaterEq it.tilTidspunktStartOfDay() } }
            filter.tom?.let { query.andWhere { mottattAvNav lessEq it.tilTidspunktEndOfDay() } }
            query.limit(MAX_ANTALL_I_RESPONS + 1) // Legg på en, for å kunne sjekke om det faktisk finnes flere enn max antall
            query
                .map { it.toSykmelding() }
        }

    private fun ResultRow.toSykmelding(): SykmeldingResponse =
        SykmeldingResponse(
            this[SykmeldingEntitet.id],
            SykmeldingDTO(
                id = this[sykmeldingId].toString(),
                orgnr = this[orgnr],
                fnr = this[fnr],
                sendSykmeldingAivenKafkaMessage = this[sendSykmeldingAivenKafkaMessage],
                sykmeldtNavn = this[sykmeldtNavn],
                mottattAvNav = this[mottattAvNav],
            ),
        )
}
