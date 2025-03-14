package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.aarsakInnsending
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.foresporselid
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.innsendt
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.orgnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.skjema
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.statusMelding
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.typeInnsending
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.versjon
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
import java.time.LocalDateTime

class InntektsmeldingRepository(
    private val db: Database,
) {
    fun opprettInntektsmeldingFraSimba(
        im: Inntektsmelding,
        org: String,
        sykmeldtFnr: String,
        innsendtDato: LocalDateTime,
        forespoerselID: String?,
    ): Int {
        sikkerLogger().info("Lagrer inntektsmelding")
        return transaction(db) {
            InntektsmeldingEntitet.insert {
                it[dokument] = im
                it[orgnr] = org
                it[fnr] = sykmeldtFnr
                it[foresporselid] = forespoerselID
                it[innsendt] = innsendtDato
                it[skjema] = SkjemaInntektsmelding(im.type.id, im.avsender.tlf, im.agp, im.inntekt, im.refusjon)
                it[aarsakInnsending] = im.aarsakInnsending
                it[typeInnsending] = InnsendingType.from(im.type)
                it[navReferanseId] = im.type.id
                it[versjon] = 1 // TODO: bør legges til i dokument-payload..
                it[avsenderSystemNavn] = "NAV_NO_SIMBA"
                it[avsenderSystemVersjon] = "1.0" // Bør egentlig komme fra simba..
                it[status] = InnsendingStatus.GODKJENT // Alt fra Simba er OK!
            }[InntektsmeldingEntitet.id]
        }
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
                        (if (request.foresporselId != null) foresporselid eq request.foresporselId else Op.TRUE) and
                        (request.fraTid?.let { innsendt greaterEq it } ?: Op.TRUE) and
                        (request.tilTid?.let { innsendt lessEq it } ?: Op.TRUE)
                }.map { it.toExposedInntektsmelding() }
        }

    private fun ResultRow.toExposedInntektsmelding(): InnsendtInntektsmelding =
        InnsendtInntektsmelding(
            skjema = this[skjema],
            orgnr = this[orgnr],
            fnr = this[fnr],
            innsendtTid = this[innsendt],
            aarsakInnsending = this[aarsakInnsending],
            typeInnsending = this[typeInnsending],
            versjon = this[versjon],
            status = this[status],
            statusMelding = this[statusMelding],
        )
}
