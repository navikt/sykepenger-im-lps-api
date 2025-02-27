package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(TransactionalExtension::class)
class InntektsmeldingRepositoryTest {
    val db = Database.init()
    val repository = InntektsmeldingRepository(db)

    @Test
    fun `opprett should insert a ny inntektsmelding`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId)
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"
        repository.opprett(inntektsmeldingJson, org, sykmeldtFnr, innsendtDato, forespoerselId)

        val result = repository.hent(org)[0]

        assertEquals(inntektsmeldingJson, result.dokument)
        assertEquals(org, result.orgnr)
        assertEquals(sykmeldtFnr, result.fnr)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId)
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
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
        assertEquals(forespoerselId, result[0].foresporsel_id)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr and request`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId)
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
                    foresporsel_id = forespoerselId,
                    fra_dato = innsendtDato.minusDays(1),
                    til_dato = innsendtDato.plusDays(1),
                ),
            )

        assertEquals(1, result.size)
        assertEquals(org, result[0].orgnr)
        assertEquals(sykmeldtFnr, result[0].fnr)
        assertEquals(forespoerselId, result[0].foresporsel_id)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr and request with no match`() {
        val org1 = "123456789"
        val sykmeldtFnr1 = "10107400090"
        val innsendtDato1 = LocalDateTime.of(2023, 1, 1, 0, 0)
        val forespoerselId1 = UUID.randomUUID().toString()
        generateTestData(org1, sykmeldtFnr1, innsendtDato1, forespoerselId1)

        val org2 = "987654321"
        val sykmeldtFnr2 = "10107400091"
        val innsendtDato2 = LocalDateTime.of(2023, 1, 2, 0, 0)
        val forespoerselId2 = UUID.randomUUID().toString()
        generateTestData(org2, sykmeldtFnr2, innsendtDato2, forespoerselId2)

        val result =
            repository.hent(
                "987654321",
                InntektsmeldingRequest(
                    fnr = null,
                    foresporsel_id = null,
                    fra_dato = null,
                    til_dato = null,
                ),
            )

        assertEquals(1, result.size)
    }

    private fun generateTestData(
        org: String,
        sykmeldtFnr: String,
        innsendtDato: LocalDateTime,
        forespoerselId: String,
    ) {
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId)
        repository.opprett(
            inntektsmeldingJson,
            org,
            sykmeldtFnr,
            innsendtDato,
            forespoerselId,
        )
    }
}
