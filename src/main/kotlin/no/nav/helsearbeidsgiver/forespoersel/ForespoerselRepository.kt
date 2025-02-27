package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.dokument
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.fnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.orgnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.status
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

class ForespoerselRepository(
    private val db: Database,
) {
    fun lagreForespoersel(
        forespoerselId: String,
        payload: ForespoerselDokument,
    ) {
        val f = hentForespoersel(forespoerselId)
        if (f != null) {
            sikkerLogger().warn("Duplikat id: $forespoerselId, kan ikke lagre")
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
        sikkerLogger().info("Forespørsel $forespoerselId lagret")
    }

    fun hentForespoersel(forespoerselId: String): Forespoersel? =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { forespoersel eq forespoerselId }
                .map {
                    it.toExposedforespoersel()
                }.getOrNull(0)
        }

    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { ForespoerselEntitet.orgnr eq orgnr }
                .map {
                    Forespoersel(
                        forespoersel_id = it[forespoersel],
                        orgnr = it[ForespoerselEntitet.orgnr],
                        fnr = it[fnr],
                        status = it[status],
                        dokument = jsonMapper.decodeFromString<ForespoerselDokument>(it[dokument]),
                    )
                }
        }

    fun filtrerForespoersler(
        consumerOrgnr: String,
        request: ForespoerselRequest,
    ): List<Forespoersel> =
        transaction(db) {
            addLogger(StdOutSqlLogger)
            ForespoerselEntitet
                .selectAll()
                .where {
                    (orgnr eq consumerOrgnr) and
                        (if (!request.fnr.isNullOrBlank()) fnr eq request.fnr else Op.TRUE) and
                        (if (!request.forespoersel_id.isNullOrBlank()) forespoersel eq request.forespoersel_id else Op.TRUE) and
                        (if (request.status != null) status eq request.status else Op.TRUE)
                }.map {
                    it.toExposedforespoersel()
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

    private fun ResultRow.toExposedforespoersel() =
        Forespoersel(
            forespoersel_id = this[forespoersel],
            orgnr = this[orgnr],
            fnr = this[fnr],
            status = this[status],
            dokument = jsonMapper.decodeFromString<ForespoerselDokument>(this[dokument]),
        )
}
