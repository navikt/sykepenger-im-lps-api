package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soknad.SoeknadEntitet.soeknadId
import no.nav.helsearbeidsgiver.soknad.SoeknadEntitet.sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class SoeknadRepository(
    private val db: Database,
) {
    fun lagreSoknad(soknad: LagreSoknad) {
        try {
            transaction(db) {
                SoeknadEntitet.insert {
                    it[soeknadId] = soknad.soknadId
                    it[sykmeldingId] = soknad.sykmeldingId
                    it[fnr] = soknad.fnr
                    it[orgnr] = soknad.orgnr
                    it[sykepengesoeknad] = soknad.sykepengesoknad
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre sykepengesøknad  med id ${soknad.soknadId} i databasen", e)
            throw e
        }
    }

    fun hentSoeknader(orgnr: String): List<SykepengesoknadDTO> =
        transaction(db) {
            SoeknadEntitet
                .selectAll()
                .where { SoeknadEntitet.orgnr eq orgnr }
                .map { it[sykepengesoeknad] }
        }

    fun hentSoeknad(id: UUID): SykepengesoknadDTO? =
        transaction(db) {
            SoeknadEntitet
                .selectAll()
                .where { soeknadId eq id }
                .map { it[sykepengesoeknad] }
                .firstOrNull()
        }
}
