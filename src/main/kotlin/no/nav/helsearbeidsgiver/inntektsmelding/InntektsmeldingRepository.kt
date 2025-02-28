package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.dokument
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.foresporselid
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.innsendt
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.mottattEvent
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.orgnr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class InntektsmeldingRepository(
    private val db: Database,
) {
    fun opprett(
        im: no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding,
        org: String,
        sykmeldtFnr: String,
        innsendtDato: LocalDateTime,
        forespoerselID: String?,
    ): Int =
        transaction(db) {
            InntektsmeldingEntitet.insert {
                it[dokument] = im
                it[orgnr] = org
                it[fnr] = sykmeldtFnr
                it[foresporselid] = forespoerselID
                it[innsendt] = innsendtDato
            }[InntektsmeldingEntitet.id]
        }

    fun hent(orgNr: String): List<InnsendtInntektsmelding> =
        transaction(db) {
            InntektsmeldingEntitet
                .selectAll()
                .where { orgnr eq orgNr }
                .map { it.toExposedInntektsmelding() }
        }

    fun hent(
        orgNr: String,
        request: InntektsmeldingRequest,
    ): List<InnsendtInntektsmelding> =
        transaction(db) {
            addLogger(StdOutSqlLogger)
            InntektsmeldingEntitet
                .selectAll()
                .where {
                    (orgnr eq orgNr) and
                        (if (request.fnr != null) fnr eq request.fnr else Op.TRUE) and
                        (if (request.foresporsel_id != null) foresporselid eq request.foresporsel_id else Op.TRUE) and
                        (request.fra_dato?.let { innsendt greaterEq it } ?: Op.TRUE) and
                        (request.til_dato?.let { innsendt lessEq it } ?: Op.TRUE)
                }.map { it.toExposedInntektsmelding() }
        }

    private fun ResultRow.toExposedInntektsmelding(): InnsendtInntektsmelding =
        InnsendtInntektsmelding(
            dokument = this[dokument],
            orgnr = this[orgnr],
            fnr = this[fnr],
            foresporsel_id = this[foresporselid],
            innsendt_tid = this[innsendt],
            mottatt_tid = this[mottattEvent],
        )
}
