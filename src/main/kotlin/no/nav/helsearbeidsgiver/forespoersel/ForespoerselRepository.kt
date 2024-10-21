package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.fnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.orgnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.status
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class ForespoerselRepository(
    private val db: Database,
) {
    fun lagreForespoersel(
        forespoerselId: String,
        organisasjonsnummer: String,
        foedselsnr: String,
    ) {
        transaction(db) {
            ForespoerselEntitet.insert {
                it[this.forespoersel] = forespoerselId
                it[orgnr] = organisasjonsnummer
                it[fnr] = foedselsnr
                it[opprettet] = LocalDateTime.now()
                it[status] = "ny"
            }
        }
    }

    fun hentForespoersel(forespoerselId: String): Forespoersel? =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { ForespoerselEntitet.forespoersel eq forespoerselId }
                .map {
                    Forespoersel(
                        forespoerselId = it[forespoersel],
                        orgnr = it[orgnr],
                        fnr = it[fnr],
                        status = it[status],
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
                    )
                }
        }
}
