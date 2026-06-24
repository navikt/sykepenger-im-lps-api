package no.nav.helsearbeidsgiver.soeknad

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.pdl.FantIkkePersonException
import no.nav.helsearbeidsgiver.pdl.PdlService
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.pdl.domene.PersonNavn
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.sykepengesoeknad
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.sykmelding.tilSykmeldingDTO
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@WithPostgresContainer
class SoeknadServiceTest {
    private lateinit var db: Database
    private lateinit var soeknadService: SoeknadService
    private lateinit var soeknadRepository: SoeknadRepository

    private lateinit var dokumentkoblingService: DokumentkoblingService
    private lateinit var sykmeldingService: SykmeldingService
    private lateinit var pdlService: PdlService

    private val orgnr = Orgnr.genererGyldig()
    private val fullPerson =
        FullPerson(
            navn =
                PersonNavn(
                    fornavn = "Test",
                    mellomnavn = "",
                    etternavn = "Testesen",
                ),
            foedselsdato = LocalDate.of(2000, 1, 1),
        )

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        soeknadRepository = SoeknadRepository(db)
        dokumentkoblingService = mockk<DokumentkoblingService>()
        sykmeldingService = mockk<SykmeldingService>()
        pdlService = mockk<PdlService>()
        soeknadService = SoeknadService(soeknadRepository, sykmeldingService, dokumentkoblingService, pdlService)
    }

    @BeforeEach
    fun clean() {
        transaction(db) { SoeknadEntitet.deleteAll() }

        clearAllMocks()
        every { dokumentkoblingService.produserSykepengesoeknadKobling(any(), any(), orgnr) } just Runs

        every { pdlService.hentFullPerson(any(), any()) } returns fullPerson
    }

    @Test
    fun `skal lagre søknad som skal sendes til arbeidsgiver`() {
        val soeknad = soeknadMock().medOrgnr(orgnr)

        soeknadService.behandleSoeknad(soeknad)

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe soeknad
    }

    @Test
    fun `søknad som skal sendes til arbeidsgiver sendes videre til hag-dialog`() {
        val soeknad = soeknadMock().medOrgnr(orgnr)

        soeknadService.behandleSoeknad(soeknad)

        verify(exactly = 1) {
            dokumentkoblingService.produserSykepengesoeknadKobling(
                soeknad.id,
                soeknad.sykmeldingId.shouldNotBeNull(),
                orgnr,
            )
        }
    }

    @Test
    fun `skal _ikke_ lagre eller videresende søknad dersom den mangler sykmeldingId`() {
        val soeknad =
            soeknadMock().copy(sykmeldingId = null)

        soeknadService.behandleSoeknad(soeknad)

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }
        lagretSoeknad shouldBe null

        verify { dokumentkoblingService wasNot Called }
    }

    @Test
    fun `skal _ikke_ lagre eller videresende søknad dersom den mangler orgnr`() {
        val soeknad = soeknadMock()

        val soeknad1 =
            soeknad.copy(arbeidsgiver = null)
        val soeknad2 =
            soeknad.copy(arbeidsgiver = soeknad.arbeidsgiver?.copy(orgnummer = null))

        soeknadService.behandleSoeknad(soeknad1)
        soeknadService.behandleSoeknad(soeknad2)

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe null

        verify { dokumentkoblingService wasNot Called }
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den er en søknadstype som ikke skal sendes til arbeidsgiver`() {
        val soeknad = soeknadMock()

        val soeknadstyperSomIkkeSkalLagres =
            SykepengeSoeknadKafkaMelding.SoknadstypeDTO.entries
                .minus(
                    listOf(
                        SykepengeSoeknadKafkaMelding.SoknadstypeDTO.ARBEIDSTAKERE,
                        SykepengeSoeknadKafkaMelding.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                        SykepengeSoeknadKafkaMelding.SoknadstypeDTO.BEHANDLINGSDAGER,
                    ),
                )

        val soeknaderSomIkkeSkalLagres = soeknadstyperSomIkkeSkalLagres.map { soeknad.copy(type = it) }

        soeknaderSomIkkeSkalLagres.forEach { soeknadService.behandleSoeknad(it) }

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe null

        verify { dokumentkoblingService wasNot Called }
    }

    @Test
    fun `skal kun behandle søknad for søknadstype GRADERT_REISETILSKUDD eller BEHANDLINGSDAGER dersom arbeidssituasjon ARBEIDSTAKER`() {
        val soeknad = soeknadMock().medOrgnr(orgnr)

        val idSomSkalLagres1 = UUID.randomUUID()
        val idSomSkalLagres2 = UUID.randomUUID()

        val soeknaderSomSkalLagres =
            listOf(
                soeknad.copy(
                    id = idSomSkalLagres1,
                    type = SykepengeSoeknadKafkaMelding.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                    arbeidssituasjon = SykepengeSoeknadKafkaMelding.ArbeidssituasjonDTO.ARBEIDSTAKER,
                ),
                soeknad.copy(
                    id = idSomSkalLagres2,
                    type = SykepengeSoeknadKafkaMelding.SoknadstypeDTO.BEHANDLINGSDAGER,
                    arbeidssituasjon = SykepengeSoeknadKafkaMelding.ArbeidssituasjonDTO.ARBEIDSTAKER,
                ),
            )

        val arbeidssituasjonerSomIkkeSkalLagres =
            SykepengeSoeknadKafkaMelding.ArbeidssituasjonDTO.entries
                .minus(SykepengeSoeknadKafkaMelding.ArbeidssituasjonDTO.ARBEIDSTAKER)

        val soeknaderSomIkkeSkalLagres =
            arbeidssituasjonerSomIkkeSkalLagres.map {
                soeknad.copy(
                    id = UUID.randomUUID(),
                    type = SykepengeSoeknadKafkaMelding.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                    arbeidssituasjon = it,
                )
            } +
                arbeidssituasjonerSomIkkeSkalLagres.map {
                    soeknad.copy(
                        id = UUID.randomUUID(),
                        type = SykepengeSoeknadKafkaMelding.SoknadstypeDTO.BEHANDLINGSDAGER,
                        arbeidssituasjon = it,
                    )
                }

        (soeknaderSomSkalLagres + soeknaderSomIkkeSkalLagres).forEach { soeknadService.behandleSoeknad(it) }

        val lagredeSoeknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoeknader.size shouldBe 2
        lagredeSoeknader.map { it?.id }.toSet() shouldBe setOf(idSomSkalLagres1, idSomSkalLagres2)

        verify(exactly = 2) {
            dokumentkoblingService.produserSykepengesoeknadKobling(
                match { it == idSomSkalLagres1 || it == idSomSkalLagres2 },
                any(),
                orgnr,
            )
        }
    }

    @Test
    fun `skal _ikke_ lagre eller videresende søknad dersom den ettersendt til Nav`() {
        val soeknad = soeknadMock()

        val soeknadSomIkkeSkalLagres =
            soeknad.copy(sendtArbeidsgiver = LocalDateTime.now().minusDays(1), sendtNav = LocalDateTime.now())

        soeknadService.behandleSoeknad(soeknadSomIkkeSkalLagres)

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe null

        verify { dokumentkoblingService wasNot Called }
    }

    @Test
    fun `skal _ikke_ lagre eller videresende søknad dersom statusen er noe annet enn sendt`() {
        val soeknad = soeknadMock()

        val statuserSomIkkeSkalLagres =
            SykepengeSoeknadKafkaMelding.SoknadsstatusDTO.entries.minus(SykepengeSoeknadKafkaMelding.SoknadsstatusDTO.SENDT)

        val soeknaderSomIkkeSkalLagres =
            statuserSomIkkeSkalLagres.map { soeknad.copy(id = UUID.randomUUID(), status = it) }

        soeknaderSomIkkeSkalLagres.forEach { soeknadService.behandleSoeknad(it) }

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe null

        verify { dokumentkoblingService wasNot Called }
    }

    @Test
    fun `skal lagre, men ikke videresende søknad dersom feltet sendtArbeidsgiver er null`() {
        val soeknad = soeknadMock()

        val soeknadSomSkalLagresMenIkkeVideresendes =
            soeknad.copy(id = UUID.randomUUID(), sendtArbeidsgiver = null)

        soeknadService.behandleSoeknad(soeknadSomSkalLagresMenIkkeVideresendes)

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe soeknadSomSkalLagresMenIkkeVideresendes

        verify { dokumentkoblingService wasNot Called }
    }

    @Test
    fun `skal _ikke_ lagre, kun videresende søknad dersom det allerede finnes en søknad i databasen med den IDen`() {
        val soeknad = soeknadMock().medOrgnr(orgnr)
        val soeknadId = UUID.randomUUID()

        val soeknadSomSkalLagres = soeknad.medId(id = soeknadId)

        val soeknadSomIkkeSkalLagres =
            soeknad.copy(id = soeknadId, fom = soeknad.fom?.minusDays(1))

        every { sykmeldingService.hentInternSykmelding(soeknad.sykmeldingId!!) } returns sykmeldingMock().tilSykmeldingDTO()
        soeknadService.behandleSoeknad(soeknadSomSkalLagres)
        soeknadService.behandleSoeknad(soeknadSomIkkeSkalLagres)

        val lagredeSoeknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoeknader.size shouldBe 1
        lagredeSoeknader.first()?.fom shouldBe soeknadSomSkalLagres.fom

        // Vi sender til dialog-app hver gang, selv om vi ikke lagrer duplikat søknad - dialog-app har egen duplikatsjekk.
        verify(exactly = 2) {
            pdlService.hentFullPerson(soeknadSomIkkeSkalLagres.fnr, soeknadSomIkkeSkalLagres.sykmeldingId!!)
            dokumentkoblingService.produserSykepengesoeknadKobling(
                soeknadId = soeknadSomSkalLagres.id,
                sykmeldingId = soeknadSomSkalLagres.sykmeldingId.shouldNotBeNull(),
                orgnr = orgnr,
            )
            dokumentkoblingService.produserSykmeldingKobling(soeknadSomSkalLagres.sykmeldingId, any(), fullPerson)
        }
    }

    @Test
    fun `forsøker å finne sykmelding og opprette sykmelding-dialog når vi mottar søknad på kafka, ignorerer at sykmelding mangler`() {
        // midlertidig fix i søknad-mottak: Dialog-app mangler noen sykmeldinger pga tidligere feil, derfor søker vi opp sykmelding og
        // ber om å opprette dialog for sikkerhets skyld
        // Hvis vi allerede har dialog, ignoreres dette av dialog-appen.
        // Hvis vi ikke har mottatt sykmeldingen enda, eller feiler ved pdl-oppslag, ignorerer vi dette, og sender søknad til dialog-app likevel,
        // siden vi mest sannsynlig har sendt sykmelding tidligere, eller så kommer sykmelding faktisk senere og da løser dialog-app dette selv.

        val soeknad = soeknadMock().medOrgnr(orgnr)
        val soeknadId = UUID.randomUUID()
        val soeknadSomSkalLagres = soeknad.medId(id = soeknadId)

        every { sykmeldingService.hentInternSykmelding(soeknad.sykmeldingId!!) } returns null

        soeknadService.behandleSoeknad(soeknadSomSkalLagres)

        val lagredeSoeknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoeknader.size shouldBe 1
        lagredeSoeknader.first()?.fom shouldBe soeknadSomSkalLagres.fom

        verify(exactly = 1) {
            sykmeldingService.hentInternSykmelding(soeknad.sykmeldingId!!)
            dokumentkoblingService.produserSykepengesoeknadKobling(
                soeknadId = soeknadSomSkalLagres.id,
                sykmeldingId = soeknadSomSkalLagres.sykmeldingId.shouldNotBeNull(),
                orgnr = orgnr,
            )
        }
        verify(exactly = 0) {
            pdlService.hentFullPerson(soeknadSomSkalLagres.fnr, soeknadSomSkalLagres.sykmeldingId!!)
            dokumentkoblingService.produserSykmeldingKobling(any(), any(), any())
        }
    }

    @Test
    fun `forsøker å finne sykmelding og opprette sykmelding-dialog når vi mottar søknad på kafka, ignorerer PDL-feil`() {
        // midlertidig fix i søknad-mottak: Dialog-app mangler noen sykmeldinger pga tidligere feil, derfor søker vi opp sykmelding og
        // ber om å opprette dialog for sikkerhets skyld
        // Hvis vi allerede har dialog, ignoreres dette av dialog-appen.
        // Hvis vi ikke har mottatt sykmeldingen enda, eller feiler ved pdl-oppslag, ignorerer vi dette, og sender søknad til dialog-app likevel,
        // siden vi mest sannsynlig har sendt sykmelding tidligere, eller så kommer sykmelding faktisk senere og da løser dialog-app dette selv.

        val soeknad = soeknadMock().medOrgnr(orgnr)
        val soeknadId = UUID.randomUUID()

        val soeknadSomSkalLagres = soeknad.medId(id = soeknadId)

        every { sykmeldingService.hentInternSykmelding(soeknad.sykmeldingId!!) } returns sykmeldingMock().tilSykmeldingDTO()
        every { pdlService.hentFullPerson(any(), any()) } throws (FantIkkePersonException(soeknad.fnr, soeknad.sykmeldingId!!))
        soeknadService.behandleSoeknad(soeknadSomSkalLagres)

        val lagredeSoeknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoeknader.size shouldBe 1
        lagredeSoeknader.first()?.fom shouldBe soeknadSomSkalLagres.fom

        verify(exactly = 1) {
            sykmeldingService.hentInternSykmelding(soeknad.sykmeldingId!!)
            pdlService.hentFullPerson(soeknadSomSkalLagres.fnr, soeknadSomSkalLagres.sykmeldingId!!)
            dokumentkoblingService.produserSykepengesoeknadKobling(
                soeknadId = soeknadSomSkalLagres.id,
                sykmeldingId = soeknadSomSkalLagres.sykmeldingId.shouldNotBeNull(),
                orgnr = orgnr,
            )
        }
        verify(exactly = 0) {
            dokumentkoblingService.produserSykmeldingKobling(any(), any(), any())
        }
    }

    fun SykepengeSoeknadKafkaMelding.medOrgnr(orgnr: Orgnr) =
        this.copy(
            arbeidsgiver =
                SykepengeSoeknadKafkaMelding.ArbeidsgiverDTO(
                    this.arbeidsgiver?.navn,
                    orgnr.toString(),
                ),
        )
}
