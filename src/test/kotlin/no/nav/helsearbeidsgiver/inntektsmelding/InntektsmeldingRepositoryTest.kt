package no.nav.helsearbeidsgiver.inntektsmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.tilSkjema
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

@WithPostgresContainer
class InntektsmeldingRepositoryTest {
    lateinit var db: Database

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
    }

    @AfterEach
    fun afterEach() {
        transaction(db) {
            InntektsmeldingEntitet.deleteAll()
        }
    }

    @Test
    fun `opprett should insert a ny inntektsmelding`() {
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingId = UUID.randomUUID()
        val forespoerselId = UUID.randomUUID()
        val inntektsmeldingJson = buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        val forventetSkjema = inntektsmeldingJson.tilSkjema()
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
        )

        val result = repository.hent(DEFAULT_ORG)[0]

        assertEquals(forventetSkjema.forespoerselId, result.navReferanseId)
        assertEquals(inntektsmeldingJson.id, result.id)
        assertEquals(forventetSkjema.agp, result.agp)
        assertEquals(forventetSkjema.inntekt, result.inntekt)
        assertEquals(forventetSkjema.refusjon, result.refusjon)
        assertEquals(DEFAULT_ORG, result.arbeidsgiver.orgnr)
        assertEquals(DEFAULT_FNR, result.sykmeldtFnr)
    }

    @Test
    fun `opprett skal ikke kunne lagre samme inntektsmelding (id) to ganger`() {
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingId = UUID.randomUUID()
        val forespoerselId = UUID.randomUUID()
        val inntektsmeldingJson = buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)

        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
        )
        val result = repository.hent(DEFAULT_ORG)
        assertEquals(1, result.size)

        assertThrows<ExposedSQLException> {
            repository.opprettInntektsmelding(
                im = inntektsmeldingJson,
            )
        }

        val result2 = repository.hent(DEFAULT_ORG)
        assertEquals(1, result2.size)
        assertEquals(inntektsmeldingJson.id, result2[0].id)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgnr`() {
        val repository = InntektsmeldingRepository(db)
        val forespoerselId = UUID.randomUUID()
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmeldingJson =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        repository.opprettInntektsmelding(
            inntektsmeldingJson,
        )

        val result = repository.hent(DEFAULT_ORG)

        assertEquals(1, result.size)
        assertEquals(DEFAULT_ORG, result[0].arbeidsgiver.orgnr)
        assertEquals(DEFAULT_FNR, result[0].sykmeldtFnr)
        assertEquals(inntektsmeldingId, result[0].id)
        assertEquals(forespoerselId, result[0].navReferanseId)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgnr and request`() {
        val repository = InntektsmeldingRepository(db)
        val forespoerselId = UUID.randomUUID()
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId = forespoerselId)
        val innsendtDato = LocalDate.now()
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson.copy(mottatt = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC)),
        )

        val result =
            repository.hent(
                DEFAULT_ORG,
                InntektsmeldingFilterRequest(
                    fnr = DEFAULT_FNR,
                    navReferanseId = forespoerselId,
                    fom = innsendtDato.minusDays(1),
                    tom = innsendtDato.plusDays(1),
                ),
            )

        assertEquals(1, result.size)
        assertEquals(DEFAULT_ORG, result[0].arbeidsgiver.orgnr)
        assertEquals(DEFAULT_FNR, result[0].sykmeldtFnr)
        assertEquals(forespoerselId, result[0].navReferanseId)
    }

    @Test
    fun `hent should return list av inntektsmeldinger by orgnr and request with no match`() {
        val repository = InntektsmeldingRepository(db)
        val forespoerselId1 = UUID.randomUUID()
        generateTestData(
            org = Orgnr(DEFAULT_ORG),
            sykmeldtFnr = Fnr(DEFAULT_FNR),
            forespoerselId = forespoerselId1,
        )

        val org2 = Orgnr.genererGyldig()
        val sykmeldtFnr2 = Fnr.genererGyldig()
        val forespoerselId2 = UUID.randomUUID()
        generateTestData(
            org = org2,
            sykmeldtFnr = sykmeldtFnr2,
            forespoerselId = forespoerselId2,
        )

        val result =
            repository.hent(
                org2.verdi,
                InntektsmeldingFilterRequest(
                    fnr = null,
                    navReferanseId = null,
                    fom = null,
                    tom = null,
                ),
            )

        assertEquals(1, result.size)
    }

    @Test
    fun `oppdater inntektsmelding med ny status`() {
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingId = UUID.randomUUID()
        val forespoerselId = UUID.randomUUID()
        val inntektsmelding1 =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        val inntektsmelding2 = buildInntektsmelding()
        repository.opprettInntektsmelding(
            im = inntektsmelding1,
            innsendingStatus = InnsendingStatus.MOTTATT,
        )
        repository.opprettInntektsmelding(
            im = inntektsmelding2,
            innsendingStatus = InnsendingStatus.MOTTATT,
        )
        val result = repository.hent(DEFAULT_ORG)
        result[0].status shouldBe InnsendingStatus.MOTTATT
        repository.oppdaterStatus(inntektsmelding1, nyStatus = InnsendingStatus.GODKJENT)
        val oppdatertInntektsmelding =
            repository.hent(
                DEFAULT_ORG,
                request = InntektsmeldingFilterRequest(navReferanseId = forespoerselId),
            )[0]
        oppdatertInntektsmelding.status shouldBe InnsendingStatus.GODKJENT
        val ikkeOppdatertInntektsmelding =
            repository.hent(
                DEFAULT_ORG,
                request = InntektsmeldingFilterRequest(navReferanseId = inntektsmelding2.type.id),
            )[0]
        ikkeOppdatertInntektsmelding.status shouldBe InnsendingStatus.MOTTATT
    }

    private fun generateTestData(
        org: Orgnr,
        sykmeldtFnr: Fnr,
        forespoerselId: UUID,
    ) {
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingJson = buildInntektsmelding(forespoerselId = forespoerselId, orgnr = org, sykemeldtFnr = sykmeldtFnr)
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
        )
    }

    @Test
    fun `hent should return list of inntektsmeldinger by navReferanseId`() {
        val repository = InntektsmeldingRepository(db)
        val forespoerselId = UUID.randomUUID()
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmeldingJson =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        repository.opprettInntektsmelding(
            inntektsmeldingJson,
        )
        val result = repository.hent(navReferanseId = forespoerselId)
        assertEquals(1, result.size)
        assertEquals(DEFAULT_ORG, result[0].arbeidsgiver.orgnr)
        assertEquals(DEFAULT_FNR, result[0].sykmeldtFnr)
        assertEquals(inntektsmeldingId, result[0].id)
        assertEquals(forespoerselId, result[0].navReferanseId)
    }
}
