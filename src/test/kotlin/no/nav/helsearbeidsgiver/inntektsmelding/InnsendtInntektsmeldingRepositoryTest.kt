package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.tilSkjema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(TransactionalExtension::class)
class InnsendtInntektsmeldingRepositoryTest {
    val db = DbConfig.init()
    val repository = InntektsmeldingRepository(db)

    @Test
    fun `opprett should insert a ny inntektsmelding`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId)
        val forventetSkjema = inntektsmeldingJson.tilSkjema()
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"
        repository.opprettInntektsmeldingFraSimba(inntektsmeldingJson, org, sykmeldtFnr, innsendtDato, forespoerselId)

        val result = repository.hent(org)[0]

        assertEquals(forventetSkjema.forespoerselId, result.navReferanseId)
        assertEquals(forventetSkjema.agp, result.agp)
        assertEquals(forventetSkjema.inntekt, result.inntekt)
        assertEquals(forventetSkjema.refusjon, result.refusjon)
        assertEquals(org, result.arbeidsgiver.orgnr)
        assertEquals(sykmeldtFnr, result.sykmeldtFnr)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr`() {
        val forespoerselId = UUID.randomUUID()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId.toString())
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"
        repository.opprettInntektsmeldingFraSimba(
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
        assertEquals(forespoerselId, result[0].navReferanseId)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgNr and request`() {
        val forespoerselId = UUID.randomUUID().toString()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId)
        val org = "123456789"
        val sykmeldtFnr = "10107400090"
        val innsendtDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        repository.opprettInntektsmeldingFraSimba(
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
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId)
        repository.opprettInntektsmeldingFraSimba(
            inntektsmeldingJson,
            org,
            sykmeldtFnr,
            innsendtDato,
            forespoerselId,
        )
    }
}
