package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database.dbQuery
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.dokument
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class ExposedInntektsmelding(
    val inntektsMelding: String,
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

    suspend fun hent(id: Int): ExposedInntektsmelding? =
        dbQuery {
            InntektsMeldingTable
                .selectAll()
                .where {
                    InntektsMeldingTable.id eq id
                }.map {
                    ExposedInntektsmelding(
                        inntektsMelding = it[dokument],
                    )
                }.singleOrNull()
        }

    suspend fun slett(id: Int) {
        dbQuery {
            InntektsMeldingTable.deleteWhere {
                InntektsMeldingTable.id eq id
            }
        }
    }

    suspend fun update(
        id: Int,
        im: ExposedInntektsmelding,
    ) {
        dbQuery {
            InntektsMeldingTable.update({ InntektsMeldingTable.id eq id }) {
                it[dokument] = im.inntektsMelding
            }
        }
    }

//    init {
//        transaction(database) {
//            SchemaUtils.create(InntektsMeldingTable)
//        }
//    }
}
