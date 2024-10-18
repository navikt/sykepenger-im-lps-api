package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.db.Database.dbQuery
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.dokument
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.fnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.foresporselid
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.innsendt
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.mottattEvent
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.orgnr
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class ExposedInntektsmelding(
    val dokument: String,
    val orgnr: String,
    val fnr: String,
    val foresporselid: String?,
    val innsendt: String,
    val mottattEvent: String,
)

class InntektsMeldingRepository {
    object InntektsMeldingTable : Table() {
        val id = integer("id").autoIncrement()
        val dokument = text("dokument")
        val orgnr = varchar("orgnr", length = 9)
        val fnr = varchar("fnr", length = 11)
        val foresporselid = varchar("foresporselid", length = 40)
        val innsendt = datetime("innsendt")
        val mottattEvent = datetime("mottatt_event")
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun opprett(
        im: String,
        org: String,
        sykmeldtFnr: String,
    ): Int =
        dbQuery {
            InntektsMeldingTable.insert {
                it[dokument] = im
                it[orgnr] = org
                it[fnr] = sykmeldtFnr
            }[InntektsMeldingTable.id]
        }

    suspend fun hent(orgNr: String): List<ExposedInntektsmelding> =
        dbQuery {
            InntektsMeldingTable
                .selectAll()
                .where {
                    orgnr eq orgNr
                }.map {
                    ExposedInntektsmelding(
                        dokument = it[dokument],
                        orgnr = it[orgnr],
                        fnr = it[fnr],
                        foresporselid = it[foresporselid],
                        innsendt = it[innsendt].toString(),
                        mottattEvent = it[mottattEvent].toString(),
                    )
                }
        }
}
