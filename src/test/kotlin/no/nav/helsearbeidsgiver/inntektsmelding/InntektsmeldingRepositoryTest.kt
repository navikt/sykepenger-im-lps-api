package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.tilSkjema
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class InntektsmeldingRepositoryTest {
    lateinit var db: Database

    @BeforeEach
    fun beforeEach() {
        db = DbConfig.init()
    }

    @Test
    fun `opprett should insert a ny inntektsmelding`() {
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingId = UUID.randomUUID().toString()
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        val forventetSkjema = inntektsmeldingJson.tilSkjema()
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
            org = org,
            sykmeldtFnr = sykmeldtFnr,
            innsendtDato = innsendtDato,
            forespoerselID = forespoerselId,
        )

        val result = repository.hent(org)[0]

        assertEquals(forventetSkjema.forespoerselId, result.navReferanseId)
        assertEquals(inntektsmeldingJson.id, result.id)
        assertEquals(forventetSkjema.agp, result.agp)
        assertEquals(forventetSkjema.inntekt, result.inntekt)
        assertEquals(forventetSkjema.refusjon, result.refusjon)
        assertEquals(org, result.arbeidsgiver.orgnr)
        assertEquals(sykmeldtFnr, result.sykmeldtFnr)
    }

    @Test
    fun `opprett skal ikke kunne lagre samme inntektsmelding (id) to ganger`() {
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingId = UUID.randomUUID().toString()
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"

        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
            org = org,
            sykmeldtFnr = sykmeldtFnr,
            innsendtDato = innsendtDato,
            forespoerselID = forespoerselId,
        )
        val result = repository.hent(org)
        assertEquals(1, result.size)

        assertThrows<ExposedSQLException> {
            repository.opprettInntektsmelding(
                im = inntektsmeldingJson,
                org = org,
                sykmeldtFnr = sykmeldtFnr,
                innsendtDato = innsendtDato,
                forespoerselID = forespoerselId,
            )
        }

        val result2 = repository.hent(org)
        assertEquals(1, result2.size)
        assertEquals(inntektsmeldingJson.id, result2[0].id)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr`() {
        val repository = InntektsmeldingRepository(db)
        val forespoerselId = UUID.randomUUID()
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmeldingJson =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId.toString(), forespoerselId = forespoerselId.toString())
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"
        repository.opprettInntektsmelding(
            inntektsmeldingJson,
            org,
            sykmeldtFnr,
            innsendtDato,
            forespoerselId.toString(),
        )

        val result = repository.hent(org)

        assertEquals(1, result.size)
        assertEquals(org, result[0].arbeidsgiver.orgnr)
        assertEquals(sykmeldtFnr, result[0].sykmeldtFnr)
        assertEquals(inntektsmeldingId, result[0].id)
        assertEquals(forespoerselId, result[0].navReferanseId)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr and request`() {
        val repository = InntektsmeldingRepository(db)
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId = forespoerselId)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
            org = org,
            sykmeldtFnr = sykmeldtFnr,
            innsendtDato = innsendtDato,
            forespoerselID = forespoerselId,
        )

        val result =
            repository.hent(
                org,
                InntektsmeldingFilterRequest(
                    fnr = sykmeldtFnr,
                    foresporselId = forespoerselId,
                    fraTid = innsendtDato.minusDays(1),
                    tilTid = innsendtDato.plusDays(1),
                ),
            )

        assertEquals(1, result.size)
        assertEquals(org, result[0].arbeidsgiver.orgnr)
        assertEquals(sykmeldtFnr, result[0].sykmeldtFnr)
        assertEquals(forespoerselId, result[0].navReferanseId.toString())
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr and request with no match`() {
        val repository = InntektsmeldingRepository(db)
        val org1 = "123456789"
        val sykmeldtFnr1 = "10107400090"
        val innsendtDato1 = LocalDateTime.of(2023, 1, 1, 0, 0)
        val forespoerselId1 = UUID.randomUUID().toString()
        generateTestData(
            org = org1,
            sykmeldtFnr = sykmeldtFnr1,
            innsendtDato = innsendtDato1,
            forespoerselId = forespoerselId1,
        )

        val org2 = "987654321"
        val sykmeldtFnr2 = "10107400091"
        val innsendtDato2 = LocalDateTime.of(2023, 1, 2, 0, 0)
        val forespoerselId2 = UUID.randomUUID().toString()
        generateTestData(
            org = org2,
            sykmeldtFnr = sykmeldtFnr2,
            innsendtDato = innsendtDato2,
            forespoerselId = forespoerselId2,
        )

        val result =
            repository.hent(
                "987654321",
                InntektsmeldingFilterRequest(
                    fnr = null,
                    foresporselId = null,
                    fraTid = null,
                    tilTid = null,
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
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId = forespoerselId)
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
            org = org,
            sykmeldtFnr = sykmeldtFnr,
            innsendtDato = innsendtDato,
            forespoerselID = forespoerselId,
        )
    }
}
