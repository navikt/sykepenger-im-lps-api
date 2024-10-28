package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import no.nav.helsearbeidsgiver.db.Database.dbQuery
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.dokument
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.fnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.foresporselid
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.innsendt
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.mottattEvent
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingEntitet.orgnr
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class InntektsMeldingRepository {
    suspend fun opprett(
        im: String,
        org: String,
        sykmeldtFnr: String,
    ): Int =
        dbQuery {
            InntektsmeldingEntitet.insert {
                it[dokument] = im
                it[orgnr] = org
                it[fnr] = sykmeldtFnr
            }[InntektsmeldingEntitet.id]
        }

    private fun ResultRow.toExposedInntektsmelding(): Inntektsmelding =
        Inntektsmelding(
            dokument = Json.parseToJsonElement(this[dokument]).jsonObject,
            orgnr = this[orgnr],
            fnr = this[fnr],
            foresporselid = this[foresporselid],
            innsendt = this[innsendt].toString(),
            mottattEvent = this[mottattEvent].toString(),
        )

    suspend fun hent(orgNr: String): List<Inntektsmelding> =
        dbQuery {
            InntektsmeldingEntitet
                .selectAll()
                .where { orgnr eq orgNr }
                .map { it.toExposedInntektsmelding() }
        }
}
