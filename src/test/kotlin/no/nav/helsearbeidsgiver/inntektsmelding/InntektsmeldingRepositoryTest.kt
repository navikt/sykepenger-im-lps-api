package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.utils.buildInnteektsmeldingJson
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class InntektsmeldingRepositoryTest {
    private lateinit var db: Database
    private lateinit var repository: InntektsmeldingRepository

    @BeforeEach
    fun setUp() {
        db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver", user = "root", password = "")
        repository = InntektsmeldingRepository(db)
        transaction(db) {
            SchemaUtils.create(InntektsmeldingEntitet)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(InntektsmeldingEntitet)
        }
    }

    @Test
    fun `opprett should insert a new inntektsmelding`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInnteektsmeldingJson(forespoerselId)
        transaction(db) {
            val org = "123456789"
            val sykmeldtFnr = "10107400090"

            val id = repository.opprett(inntektsmeldingJson, org, sykmeldtFnr, forespoerselId)

            val result = InntektsmeldingEntitet.selectAll().single()

            assertEquals(id, result[InntektsmeldingEntitet.id])
            assertEquals(inntektsmeldingJson.trim(), result[InntektsmeldingEntitet.dokument])
            assertEquals(org, result[InntektsmeldingEntitet.orgnr])
            assertEquals(sykmeldtFnr, result[InntektsmeldingEntitet.fnr])
        }
    }

    @Test
    fun `hent should return list of inntektsmeldinger by orgNr`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInnteektsmeldingJson(forespoerselId)
        transaction(db) {
            val org = "123456789"
            val sykmeldtFnr = "10107400090"
            repository.opprett(
                inntektsmeldingJson,
                org,
                sykmeldtFnr,
                forespoerselId,
            )

            val result = repository.hent(org)

            assertEquals(1, result.size)
            assertEquals(org, result[0].orgnr)
            assertEquals(sykmeldtFnr, result[0].fnr)
            assertEquals(forespoerselId, result[0].foresporselid)
        }
    }
}
