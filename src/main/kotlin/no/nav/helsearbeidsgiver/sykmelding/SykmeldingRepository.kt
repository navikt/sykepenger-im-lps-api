package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.arbeidsgiverSykmelding
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.orgnr
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sykmeldingId
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.UUID

class SykmeldingRepository(
    private val db: Database,
) {
    fun lagreSykmelding(
        id: UUID,
        fnr: String,
        orgnr: String,
        sykmelding: ArbeidsgiverSykmelding,
    ) {
        try {
            transaction(db) {
                SykmeldingEntitet.upsert(
                    keys = arrayOf(sykmeldingId),
                    onUpdate = {
                        it[sykmeldingId] = id
                    },
                    // Eksluderer alle kolonner fra oppdatering dersom sykmeldingen finnes fra før.
                    onUpdateExclude = SykmeldingEntitet.columns,
                ) {
                    it[sykmeldingId] = id
                    it[SykmeldingEntitet.fnr] = fnr
                    it[SykmeldingEntitet.orgnr] = orgnr
                    it[arbeidsgiverSykmelding] = sykmelding
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
                .where { SykmeldingEntitet.sykmeldingId eq id }
                .map { it.toSykmelding() }
                .firstOrNull()
        }

    private fun ResultRow.toSykmelding(): SykmeldingResponse =
        SykmeldingResponse(
            id = this[sykmeldingId].toString(),
            orgnr = this[orgnr],
            fnr = this[fnr],
            arbeidsgiverSykmelding = this[arbeidsgiverSykmelding],
        )
}
