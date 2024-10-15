package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database.dbQuery
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository.InntektsMeldingTable.inntektsMelding
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class ExposedInntektsmelding(
    val inntektsMelding: String,
)

class InntektsMeldingRepository {
    object InntektsMeldingTable : Table() {
        val id = integer("id").autoIncrement()
        val inntektsMelding = text("inntektsMelding")

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun opprett(im: ExposedInntektsmelding): Int =
        dbQuery {
            InntektsMeldingTable.insert {
                it[inntektsMelding] = im.inntektsMelding
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
                        inntektsMelding = it[inntektsMelding],
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
                it[inntektsMelding] = im.inntektsMelding
            }
        }
    }

//    init {
//        transaction(database) {
//            SchemaUtils.create(InntektsMeldingTable)
//        }
//    }
}
