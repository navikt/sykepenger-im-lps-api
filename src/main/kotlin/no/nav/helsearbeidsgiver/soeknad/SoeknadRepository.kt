package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.soeknadId
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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

    fun hentSoeknaderMedVedtaksperiodeId(vedtaksperiodeId: UUID): List<SykepengesoknadDTO> =
        transaction(db) {
            SoeknadEntitet
                .selectAll()
                .where {
                    SoeknadEntitet.vedtaksperiodeId eq vedtaksperiodeId
                }.map { it[sykepengesoeknad] }
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
                        }.associate { it[soeknadId] to it[SoeknadEntitet.vedtaksperiodeId] }
                loggDuplikateOgManglendeSoeknader(eksisterendeSoeknader, vedtaksperiodeId, soeknadIder)
                val soeknaderUtenVedtaksperiodeId = eksisterendeSoeknader.filter { (_, vedtaksperiodeId) -> vedtaksperiodeId == null }.keys
                if (soeknaderUtenVedtaksperiodeId.isNotEmpty()) {
                    SoeknadEntitet
                        .update(
                            where = { soeknadId inList soeknaderUtenVedtaksperiodeId },
                        ) {
                            it[SoeknadEntitet.vedtaksperiodeId] = vedtaksperiodeId
                        }
                    logger().info("Oppdaterte søknader $soeknaderUtenVedtaksperiodeId med vedtaksperiodeId: $vedtaksperiodeId")
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error(
                "Klarte ikke å oppdatere sykepengesøknader med vedtaksperiodeId $vedtaksperiodeId i databasen",
                e,
            )
            throw e
        }
    }

    private fun loggDuplikateOgManglendeSoeknader(
        eksisterendeSoeknader: Map<UUID, UUID?>,
        vedtaksperiodeId: UUID,
        soeknadIder: Set<UUID>,
    ) {
        eksisterendeSoeknader.forEach { (sId, vId) ->
            if (vId != null && vId != vedtaksperiodeId) {
                logger()
                    .warn(
                        "Fant eksisterende søknad med søknadId: $sId  og vedtaksperiodeId: $vId, oppdaterer ikke vedtaksperiodeId til: $vedtaksperiodeId for denne raden",
                    )
            }
        }
        soeknadIder.minus(eksisterendeSoeknader.keys).forEach { sId ->
            logger().warn("Fant ikke soeknadId: $sId i databasen, kan være syntetiske data")
        }
    }
}
