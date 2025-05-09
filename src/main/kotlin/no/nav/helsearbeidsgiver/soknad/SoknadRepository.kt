package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soknad.SoknadEntitet.soknadId
import no.nav.helsearbeidsgiver.soknad.SoknadEntitet.sykepengesoknad
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class SoknadRepository(
    private val db: Database,
) {
    fun lagreSoknad(soknad: LagreSoknad) {
        try {
            transaction(db) {
                SoknadEntitet.insert {
                    it[soknadId] = soknad.soknadId
                    it[sykmeldingId] = soknad.sykmeldingId
                    it[fnr] = soknad.fnr
                    it[orgnr] = soknad.orgnr
                    it[sykepengesoknad] = soknad.sykepengesoknad
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre sykepengesøknad  med id ${soknad.soknadId} i databasen", e)
            throw e
        }
    }

    fun hentSoknad(id: UUID): SykepengesoknadDTO? =
        transaction(db) {
            SoknadEntitet
                .selectAll()
                .where { soknadId eq id }
                .map { it[sykepengesoknad] }
                .firstOrNull()
        }
}
