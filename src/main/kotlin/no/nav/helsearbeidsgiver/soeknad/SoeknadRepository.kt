package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.soeknadId
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.updateReturning
import java.util.UUID

class SoeknadRepository(
    private val db: Database,
) {
    fun lagreSoeknad(soeknad: LagreSoeknad) {
        try {
            transaction(db) {
                SoeknadEntitet.insert {
                    it[soeknadId] = soeknad.soeknadId
                    it[sykmeldingId] = soeknad.sykmeldingId
                    it[fnr] = soeknad.fnr
                    it[orgnr] = soeknad.orgnr
                    it[sykepengesoeknad] = soeknad.sykepengesoeknad
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre sykepengesøknad  med id ${soeknad.soeknadId} i databasen", e)
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

    fun oppdaterSoeknaderMedVedtaksperiodeId(
        soeknadIder: Set<UUID>,
        vedtaksperiodeId: UUID,
    ) {
        try {
            transaction(db) {
                val eksisterendeSoeknader =
                    SoeknadEntitet
                        .selectAll()
                        .where {
                            soeknadId inList soeknadIder
                            SoeknadEntitet.vedtaksperiodeId.isNotNull()
                        }.associate { it[soeknadId] to it[SoeknadEntitet.vedtaksperiodeId] }
                eksisterendeSoeknader.forEach { (sId, vId) ->
                    sikkerLogger().warn(
                        "Fant eksisterende søknad med vedtaksperiode med søknadid: $sId  og vedtaksperiodeId: $vId, oppdaterer ikke denne raden",
                    )
                }
                val resterendeSoeknader = soeknadIder.minus(eksisterendeSoeknader.keys)
                if (resterendeSoeknader.isNotEmpty()) {
                    SoeknadEntitet
                        .updateReturning(
                            returning = listOf(soeknadId),
                            where = { soeknadId inList resterendeSoeknader },
                        ) {
                            it[SoeknadEntitet.vedtaksperiodeId] = vedtaksperiodeId
                        }.map { it[soeknadId] }
                        .also { logger().info("Oppdaterte søknader $it med vedtaksperiodeId: $vedtaksperiodeId") }
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å oppdatere sykepengesøknader  med vedtaksperiodeId $vedtaksperiodeId i databasen", e)
            throw e
        }
    }
}
