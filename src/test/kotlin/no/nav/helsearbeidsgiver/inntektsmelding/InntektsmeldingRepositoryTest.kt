package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.utils.buildInnteektsmeldingJson
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class InntektsmeldingRepositoryTest {
    private lateinit var db: org.jetbrains.exposed.sql.Database
    private lateinit var repository: InntektsmeldingRepository

    @BeforeEach
    fun setUp() {
        db = Database.init()
        repository = InntektsmeldingRepository(db)
    }

    @Test
    fun `opprett should insert a ny inntektsmelding`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInnteektsmeldingJson(forespoerselId)
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        transaction(db) {
            val org = "123456789"
            val sykmeldtFnr = "10107400090"

            val id = repository.opprett(inntektsmeldingJson, org, sykmeldtFnr, innsendtDato, forespoerselId)

            val result = InntektsmeldingEntitet.selectAll().single()

            assertEquals(id, result[InntektsmeldingEntitet.id])
            assertEquals(inntektsmeldingJson.trim(), result[InntektsmeldingEntitet.dokument])
            assertEquals(org, result[InntektsmeldingEntitet.orgnr])
            assertEquals(sykmeldtFnr, result[InntektsmeldingEntitet.fnr])
            rollback()
        }
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInnteektsmeldingJson(forespoerselId)
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        transaction(db) {
            val org = "123456789"
            val sykmeldtFnr = "10107400090"
            repository.opprett(
                inntektsmeldingJson,
                org,
                sykmeldtFnr,
                innsendtDato,
                forespoerselId,
            )

            val result = repository.hent(org)

            assertEquals(1, result.size)
            assertEquals(org, result[0].orgnr)
            assertEquals(sykmeldtFnr, result[0].fnr)
            assertEquals(forespoerselId, result[0].foresporselid)
            rollback()
        }
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr and request`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInnteektsmeldingJson(forespoerselId)
        transaction(db) {
            val org = "123456789"
            val sykmeldtFnr = "10107400090"
            val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
            repository.opprett(
                inntektsmeldingJson,
                org,
                sykmeldtFnr,
                innsendtDato,
                forespoerselId,
            )

            val result =
                repository.hent(
                    org,
                    InntektsmeldingRequest(
                        fnr = sykmeldtFnr,
                        foresporselid = forespoerselId,
                        datoFra = innsendtDato.minusDays(1),
                        datoTil = innsendtDato.plusDays(1),
                    ),
                )

            assertEquals(1, result.size)
            assertEquals(org, result[0].orgnr)
            assertEquals(sykmeldtFnr, result[0].fnr)
            assertEquals(forespoerselId, result[0].foresporselid)
            rollback()
        }
    }
}
