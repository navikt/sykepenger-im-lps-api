package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class InnsendingRepository(
    private val db: Database,
) {
    fun opprettInnsending(
        organisasjonsNr: String,
        lpsOrgnr: String,
        payload: SkjemaInntektsmelding,
    ): UUID {
        val dokument =
            transaction(db) {
                InnsendingEntitet.insert {
                    it[orgnr] = organisasjonsNr
                    it[lps] = lpsOrgnr
                    // TODO: Finne ut hvor vi henter fnr fra
                    it[fnr] = "fnr"
                    it[status] = InnsendingStatus.NY
                    it[dokument] = payload
                    it[foresporselid] = payload.forespoerselId
                    it[innsendtdato] = LocalDateTime.now()
                }
            }
        return dokument[InnsendingEntitet.id]
    }
}
