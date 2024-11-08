package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.dokument
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.fnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.orgnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.status
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ForespoerselRepository(
    private val db: Database,
) {
    private val logger = LoggerFactory.getLogger(ForespoerselRepository::class.java)

    fun lagreForespoersel(
        forespoerselId: String,
        payload: ForespoerselDokument,
    ) {
        val f = hentForespoersel(forespoerselId)
        if (f != null) {
            logger.warn("Duplikat id: $forespoerselId, kan ikke lagre")
            return
        }
        val organisasjonsnummer = payload.orgnr
        val foedselsnr = payload.fnr
        val jsonString = jsonMapper.encodeToString(ForespoerselDokument.serializer(), payload)
        transaction(db) {
            ForespoerselEntitet.insert {
                it[this.forespoersel] = forespoerselId
                it[orgnr] = organisasjonsnummer
                it[fnr] = foedselsnr
                it[opprettet] = LocalDateTime.now()
                it[status] = Status.AKTIV
                it[dokument] = jsonString
            }
        }
    }

    fun hentForespoersel(forespoerselId: String): Forespoersel? =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { forespoersel eq forespoerselId }
                .map {
                    Forespoersel(
                        forespoerselId = it[forespoersel],
                        orgnr = it[orgnr],
                        fnr = it[fnr],
                        status = it[status],
                        dokument = jsonMapper.decodeFromString<ForespoerselDokument>(it[dokument]),
                    )
                }.getOrNull(0)
        }

    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { ForespoerselEntitet.orgnr eq orgnr }
                .map {
                    Forespoersel(
                        forespoerselId = it[forespoersel],
                        orgnr = it[ForespoerselEntitet.orgnr],
                        fnr = it[fnr],
                        status = it[status],
                        dokument = jsonMapper.decodeFromString<ForespoerselDokument>(it[dokument]),
                    )
                }
        }

    fun settBesvart(forespoerselId: String): Int = oppdaterStatus(forespoerselId, Status.MOTTATT)

    fun settForkastet(forespoerselId: String): Int = oppdaterStatus(forespoerselId, Status.FORKASTET)

    private fun oppdaterStatus(
        forespoerselId: String,
        status: Status,
    ): Int =
        transaction(db) {
            ForespoerselEntitet.update(
                where = {
                    (forespoersel eq forespoerselId)
                },
            ) {
                it[ForespoerselEntitet.status] = status
            }
        }
}
