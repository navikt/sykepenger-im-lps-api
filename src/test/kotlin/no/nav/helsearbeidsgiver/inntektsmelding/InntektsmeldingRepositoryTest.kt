package no.nav.helsearbeidsgiver.inntektsmelding

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.soeknad.LagreSoeknad
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
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
import org.junit.jupiter.api.assertNotNull
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
        val inntektsmeldingJson =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        val forventetSkjema = inntektsmeldingJson.tilSkjema()
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
        )

        val result = repository.hent(filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG))[0]

        assertEquals(forventetSkjema.forespoerselId, result.navReferanseId)
        assertEquals(inntektsmeldingJson.id, result.id)
        assertEquals(forventetSkjema.agp, result.agp?.tilArbeidsgiverperiodeMedEgenmeldinger())
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
        val inntektsmeldingJson =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)

        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
        )
        val result = repository.hent(filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG))
        assertEquals(1, result.size)

        assertThrows<ExposedSQLException> {
            repository.opprettInntektsmelding(
                im = inntektsmeldingJson,
            )
        }

        val result2 = repository.hent(filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG))
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

        val result = repository.hent(filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG))

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
                InntektsmeldingFilter(
                    orgnr = DEFAULT_ORG,
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
                InntektsmeldingFilter(
                    orgnr = org2.verdi,
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
        val result = repository.hent(filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG))
        result[0].status shouldBe InnsendingStatus.MOTTATT
        repository.oppdaterStatus(inntektsmelding1, nyStatus = InnsendingStatus.GODKJENT)
        val oppdatertInntektsmelding =
            repository.hent(
                filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG, navReferanseId = forespoerselId),
            )[0]
        oppdatertInntektsmelding.status shouldBe InnsendingStatus.GODKJENT
        val ikkeOppdatertInntektsmelding =
            repository.hent(
                filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG, navReferanseId = inntektsmelding2.type.id),
            )[0]
        ikkeOppdatertInntektsmelding.status shouldBe InnsendingStatus.MOTTATT
    }

    @Test
    fun `oppdater inntektsmelding med feilet status`() {
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
        val result = repository.hent(filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG))
        result[0].status shouldBe InnsendingStatus.MOTTATT
        repository.oppdaterFeilstatusOgFeilkode(
            AvvistInntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                forespoerselId = forespoerselId,
                vedtaksperiodeId = inntektsmelding1.vedtaksperiodeId!!,
                orgnr = inntektsmelding1.avsender.orgnr,
                feilkode = Valideringsfeil.Feilkode.INNTEKT_AVVIKER_FRA_A_ORDNINGEN,
            ),
        )
        val oppdatertInntektsmelding =
            repository.hent(
                filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG, navReferanseId = forespoerselId),
            )[0]
        oppdatertInntektsmelding.status shouldBe InnsendingStatus.FEILET

        val ikkeOppdatertInntektsmelding =
            repository.hent(
                filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG, navReferanseId = inntektsmelding2.type.id),
            )[0]
        ikkeOppdatertInntektsmelding.status shouldBe InnsendingStatus.MOTTATT
    }

    @Test
    fun `repository skal begrense antall entiteter som returneres - kan max returnere maxLimit + 1`() {
        val repository = InntektsmeldingRepository(db)
        for (i in 1..MAX_ANTALL_I_RESPONS + 10) {
            val forespoerselId = UUID.randomUUID()
            val inntektsmeldingId = UUID.randomUUID()

            val inntektsmeldingJson =
                buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
            repository.opprettInntektsmelding(
                inntektsmeldingJson,
            )
        }
        repository.hent(filter = InntektsmeldingFilter(orgnr = DEFAULT_ORG)).size shouldBe MAX_ANTALL_I_RESPONS + 1
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

    @Test
    fun `hent med fraLoepenr skal returnere kun loepenr større enn oppgitt verdi`() {
        val repository = InntektsmeldingRepository(db)
        val inntektsmelding1 =
            buildInntektsmelding(inntektsmeldingId = UUID.randomUUID())
        val innsendingsId1 = repository.opprettInntektsmelding(inntektsmelding1)
        val inntektsmelding2 =
            buildInntektsmelding(inntektsmeldingId = UUID.randomUUID())
        repository.opprettInntektsmelding(inntektsmelding2)
        val inntektsmelding3 =
            buildInntektsmelding(inntektsmeldingId = UUID.randomUUID())
        repository.opprettInntektsmelding(inntektsmelding3)

        val inntektsmeldingLoepenr1 = repository.hentMedInnsendingId(innsendingsId1)?.loepenr ?: error("løpenr kan ikke være null")

        val result = repository.hent(InntektsmeldingFilter(orgnr = DEFAULT_ORG, fraLoepenr = inntektsmeldingLoepenr1))
        result.size shouldBe 2
        result.forEach {
            it.loepenr shouldBeGreaterThan inntektsmeldingLoepenr1
        }
    }

    @Test
    fun `hentInntektsmeldingDialogMelding skal returnere inntektsmelding for dialogmelding`() {
        val inntektsmeldingRepository = InntektsmeldingRepository(db)
        val soeknadRepository = SoeknadRepository(db)
        val forespoerselRepository = ForespoerselRepository(db)

        val sykmeldingId = UUID.randomUUID()
        val inntektsmeldingId = UUID.randomUUID()
        val forespoerselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val soeknad = soeknadMock().copy(sykmeldingId = sykmeldingId)
        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())
        soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(setOf(soeknad.id), vedtaksperiodeId)

        forespoerselRepository.lagreForespoersel(
            forespoersel = forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR, forespoerselId, vedtaksperiodeId),
            eksponertForespoerselId = forespoerselId,
        )

        val inntektsmeldingJson =
            buildInntektsmelding(inntektsmeldingId = inntektsmeldingId, forespoerselId = forespoerselId)
        inntektsmeldingRepository.opprettInntektsmelding(inntektsmeldingJson)

        val inntektsmeldingDialogMelding = inntektsmeldingRepository.hentInntektsmeldingDialogMelding(inntektsmeldingId)
        assertNotNull(inntektsmeldingDialogMelding)
        assertEquals(DEFAULT_ORG, inntektsmeldingDialogMelding.orgnr)
        assertEquals(inntektsmeldingId, inntektsmeldingDialogMelding.innsendingId)
        assertEquals(sykmeldingId, inntektsmeldingDialogMelding.sykmeldingId)
        assertEquals(forespoerselId, inntektsmeldingDialogMelding.forespoerselId)
        assertEquals(InnsendingStatus.GODKJENT, inntektsmeldingDialogMelding.status)
    }

    private fun generateTestData(
        org: Orgnr,
        sykmeldtFnr: Fnr,
        forespoerselId: UUID,
    ) {
        val repository = InntektsmeldingRepository(db)
        val inntektsmeldingJson =
            buildInntektsmelding(forespoerselId = forespoerselId, orgnr = org, sykemeldtFnr = sykmeldtFnr)
        repository.opprettInntektsmelding(
            im = inntektsmeldingJson,
        )
    }
}

fun SykepengeSoeknadKafkaMelding.tilLagreSoeknad(): LagreSoeknad =
    LagreSoeknad(
        soeknadId = id,
        sykmeldingId = sykmeldingId!!,
        fnr = fnr,
        orgnr = arbeidsgiver?.orgnummer!!,
        sykepengesoeknad = this,
    )
