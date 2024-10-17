package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database.dbQuery
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.dokument
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.fnr
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.forspoerselId
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.innsendt
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.mottattEvent
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.orgnr
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll

data class ExposedInntektsmelding(
    val inntektsMelding: String,
    val orgnr: String,
    val fnr: String,
    val forspoerselId: String,
    val innsendt: String,
    val mottattEvent: String,
)

class InntektsMeldingRepository {
    object InntektsMeldingTable : Table() {
        val id = integer("id").autoIncrement()
        val dokument = text("inntektsMelding")
        val orgnr = varchar("orgnr", length = 9)
        val fnr = varchar("fnr", length = 11)
        val forspoerselId = varchar("foresporselId", length = 40)
        val innsendt = datetime("innsendt")
        val mottattEvent = datetime("mottatt_event")
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun opprett(im: ExposedInntektsmelding): Int =
        dbQuery {
            InntektsMeldingTable.insert {
                it[dokument] = im.inntektsMelding
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
                        inntektsMelding = it[dokument],
                        orgnr = it[orgnr],
                        fnr = it[fnr],
                        forspoerselId = it[forspoerselId],
                        innsendt = it[innsendt].toString(),
                        mottattEvent = it[mottattEvent].toString(),
                    )
                }
        }
}
