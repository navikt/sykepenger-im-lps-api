package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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

    fun hentInnsendinger(): List<Innsending> =
        transaction(db) {
            InnsendingEntitet
                .selectAll()
                .map { it.toExposedInnsending() }
        }

    fun getById(id: UUID): Innsending? =
        transaction(db) {
            InnsendingEntitet
                .selectAll()
                .where(InnsendingEntitet.id eq id)
                .mapNotNull { it.toExposedInnsending() }
                .singleOrNull()
        }

    private fun ResultRow.toExposedInnsending(): Innsending =
        Innsending(
            innsendingId = this[InnsendingEntitet.id].toString(),
            orgnr = this[InnsendingEntitet.orgnr],
            lps = this[InnsendingEntitet.lps],
            fnr = this[InnsendingEntitet.fnr],
            status = this[InnsendingEntitet.status],
            dokument = this[InnsendingEntitet.dokument],
            forespoerselId = this[InnsendingEntitet.foresporselid].toString(),
            innsendtDato = this[InnsendingEntitet.innsendtdato].toString(),
            inntektsmeldingId = this[InnsendingEntitet.inntektsmeldingid],
            feilAarsak = this[InnsendingEntitet.feilaarsak] ?: "",
        )
}
