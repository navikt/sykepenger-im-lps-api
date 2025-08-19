package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.aarsakInnsending
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.avsenderSystemNavn
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.avsenderSystemVersjon
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.innsendingId
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.innsendt
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.navReferanseId
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.orgnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.skjema
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.statusMelding
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.typeInnsending
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.versjon
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilTidspunktEndOfDay
import no.nav.helsearbeidsgiver.utils.tilTidspunktStartOfDay
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class InntektsmeldingRepository(
    private val db: Database,
) {
    fun opprettInntektsmelding(
        im: Inntektsmelding,
        innsendingStatus: InnsendingStatus = InnsendingStatus.GODKJENT,
    ): UUID {
        logger().info("Lagrer inntektsmelding med id ${im.id}")
        return transaction(db) {
            InntektsmeldingEntitet.insert {
                it[innsendingId] = im.id
                it[dokument] = im
                it[orgnr] = im.avsender.orgnr.verdi
                it[fnr] = im.sykmeldt.fnr.verdi
                it[innsendt] = im.mottatt.toLocalDateTime()
                it[skjema] = SkjemaInntektsmelding(im.type.id, im.avsender.tlf, im.agp, im.inntekt, im.refusjon)
                it[aarsakInnsending] = im.aarsakInnsending
                it[typeInnsending] = InnsendingType.from(im.type)
                it[navReferanseId] = im.type.id
                it[versjon] = 1 // TODO: legges til i dokument-payload..?
                it[avsenderSystemNavn] = im.type.avsenderSystem.navn
                it[avsenderSystemVersjon] = im.type.avsenderSystem.versjon
                it[status] = innsendingStatus
            }[innsendingId]
        }
    }

    fun hent(filter: InntektsmeldingFilter): List<InntektsmeldingResponse> =
        transaction(db) {
            addLogger(StdOutSqlLogger)
            val query =
                InntektsmeldingEntitet
                    .selectAll()
                    .where { orgnr eq filter.orgnr }

            filter.status?.let { query.andWhere { status eq it } }
            filter.innsendingId?.let { query.andWhere { innsendingId eq it } }
            filter.fnr?.let { query.andWhere { fnr eq it } }
            filter.navReferanseId?.let { query.andWhere { navReferanseId eq it } }
            filter.fom?.let { query.andWhere { innsendt greaterEq it.tilTidspunktStartOfDay() } }
            filter.tom?.let { query.andWhere { innsendt lessEq it.tilTidspunktEndOfDay() } }
            query.map { it.toExposedInntektsmelding() }
        }

    fun hent(navReferanseId: UUID): List<InntektsmeldingResponse> =
        transaction(db) {
            InntektsmeldingEntitet
                .selectAll()
                .where { InntektsmeldingEntitet.navReferanseId eq navReferanseId }
                .map { it.toExposedInntektsmelding() }
        }

    fun hentMedInnsendingId(
        orgnr: String,
        innsendingId: UUID,
    ): InntektsmeldingResponse? =
        transaction(db) {
            InntektsmeldingEntitet
                .selectAll()
                .where { (InntektsmeldingEntitet.innsendingId eq innsendingId) and (InntektsmeldingEntitet.orgnr eq orgnr) }
                .map { it.toExposedInntektsmelding() }
                .firstOrNull()
        }

    fun hentMedInnsendingId(innsendingId: UUID): InntektsmeldingResponse? =
        transaction(db) {
            InntektsmeldingEntitet
                .selectAll()
                .where { (InntektsmeldingEntitet.innsendingId eq innsendingId) }
                .map { it.toExposedInntektsmelding() }
                .firstOrNull()
        }

    fun oppdaterStatus(
        inntektsmelding: Inntektsmelding,
        nyStatus: InnsendingStatus,
    ): Int =
        transaction(db) {
            InntektsmeldingEntitet.update(
                where = {
                    innsendingId eq inntektsmelding.id
                },
            ) {
                it[status] = nyStatus
            }
        }

    private fun ResultRow.toExposedInntektsmelding(): InntektsmeldingResponse =
        InntektsmeldingResponse(
            navReferanseId = this[navReferanseId],
            agp = this[skjema].agp,
            inntekt = this[skjema].inntekt,
            refusjon = this[skjema].refusjon,
            sykmeldtFnr = this[fnr],
            aarsakInnsending = this[aarsakInnsending],
            typeInnsending = this[typeInnsending],
            innsendtTid = this[innsendt],
            versjon = this[versjon],
            arbeidsgiver = Arbeidsgiver(this[orgnr], this[skjema].avsenderTlf),
            avsender = Avsender(this[avsenderSystemNavn], this[avsenderSystemVersjon]),
            status = this[status],
            statusMelding = this[statusMelding],
            id = this[innsendingId],
        )
}
