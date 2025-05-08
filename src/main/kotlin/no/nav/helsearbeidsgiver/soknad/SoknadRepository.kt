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
    fun lagreSoknad(soknad: SykepengesoknadDTO) {
        try {
            val validertSoknad = soknad.validerPaakrevdeFelter()
            transaction(db) {
                SoknadEntitet.insert {
                    it[soknadId] = validertSoknad.soknadId
                    it[sykmeldingId] = validertSoknad.sykmeldingId
                    it[fnr] = validertSoknad.fnr
                    it[orgnr] = validertSoknad.orgnr
                    it[sykepengesoknad] = validertSoknad.sykepengesoknad
                }
            }
        } catch (e: IllegalArgumentException) {
            sikkerLogger().error("Ignorerer sykepengesøknad med $soknad fordi søknaden mangler et påkrevd felt.", e)
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre sykepengesøknad $soknad i databasen: ${e.message}")
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

private fun SykepengesoknadDTO.validerPaakrevdeFelter(): LagreSoknad =
    LagreSoknad(
        soknadId = id,
        sykmeldingId = requireNotNull(sykmeldingId, { "SykmeldingId kan ikke være null" }),
        fnr = fnr,
        orgnr = requireNotNull(arbeidsgiver?.orgnummer, { "Orgnummer kan ikke være null" }),
        sykepengesoknad = this,
    )

data class LagreSoknad(
    val soknadId: UUID,
    val sykmeldingId: UUID,
    val fnr: String,
    val orgnr: String,
    val sykepengesoknad: SykepengesoknadDTO,
)
