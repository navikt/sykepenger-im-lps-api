package no.nav.helsearbeidsgiver.soeknad

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.dialogporten.DialogSykepengesoeknad
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.sykepengesoeknad
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@WithPostgresContainer
class SoeknadServiceTest {
    private lateinit var db: Database
    private lateinit var soeknadService: SoeknadService
    private lateinit var soeknadRepository: SoeknadRepository
    private lateinit var dialogportenService: DialogportenService

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        soeknadRepository = SoeknadRepository(db)
        dialogportenService = mockk<DialogportenService>()
        soeknadService = SoeknadService(soeknadRepository, dialogportenService)
    }

    @BeforeEach
    fun clean() {
        transaction(db) { SoeknadEntitet.deleteAll() }

        clearAllMocks()
        every { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) } just Runs
    }

    @Test
    fun `skal lagre søknad som skal sendes til arbeidsgiver`() {
        val soeknad =
            soeknadMock()

        soeknadService.behandleSoeknad(soeknad)

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe soeknad
    }

    @Test
    fun `søknad som skal sendes til arbeidsgiver sendes videre til hag-dialog`() {
        val soeknad =
            soeknadMock()

        soeknadService.behandleSoeknad(soeknad)

        val forventetDialogSykepengesoeknad =
            DialogSykepengesoeknad(
                soeknadId = soeknad.id,
                sykmeldingId = soeknad.sykmeldingId.shouldNotBeNull(),
                orgnr =
                    Orgnr(
                        soeknad.arbeidsgiver
                            .shouldNotBeNull()
                            .orgnummer
                            .shouldNotBeNull(),
                    ),
            )
        verify(exactly = 1) { dialogportenService.oppdaterDialogMedSykepengesoeknad(forventetDialogSykepengesoeknad) }
    }

    @Test
    fun `skal _ikke_ lagre eller videresende søknad dersom den mangler sykmeldingId`() {
        val soeknad =
            soeknadMock().copy(sykmeldingId = null)

        soeknadService.behandleSoeknad(soeknad)

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe null
        verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
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
        verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den er en søknadstype som ikke skal sendes til arbeidsgiver`() {
        val soeknad = soeknadMock()

        val soeknadstyperSomIkkeSkalLagres =
            SykepengesoknadDTO.SoknadstypeDTO.entries
                .minus(
                    listOf(
                        SykepengesoknadDTO.SoknadstypeDTO.ARBEIDSTAKERE,
                        SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                        SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER,
                    ),
                )

        val soeknaderSomIkkeSkalLagres = soeknadstyperSomIkkeSkalLagres.map { soeknad.copy(type = it) }

        soeknaderSomIkkeSkalLagres.forEach { soeknadService.behandleSoeknad(it) }

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe null
        verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
    }

    @Test
    fun `skal kun behandle søknad for søknadstype GRADERT_REISETILSKUDD eller BEHANDLINGSDAGER dersom arbeidssituasjon ARBEIDSTAKER`() {
        val soeknad = soeknadMock()

        val idSomSkalLagres1 = UUID.randomUUID()
        val idSomSkalLagres2 = UUID.randomUUID()

        val soeknaderSomSkalLagres =
            listOf(
                soeknad.copy(
                    id = idSomSkalLagres1,
                    type = SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                    arbeidssituasjon = SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER,
                ),
                soeknad.copy(
                    id = idSomSkalLagres2,
                    type = SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER,
                    arbeidssituasjon = SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER,
                ),
            )

        val arbeidssituasjonerSomIkkeSkalLagres =
            SykepengesoknadDTO.ArbeidssituasjonDTO.entries
                .minus(SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER)

        val soeknaderSomIkkeSkalLagres =
            arbeidssituasjonerSomIkkeSkalLagres.map {
                soeknad.copy(
                    id = UUID.randomUUID(),
                    type = SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                    arbeidssituasjon = it,
                )
            } +
                arbeidssituasjonerSomIkkeSkalLagres.map {
                    soeknad.copy(
                        id = UUID.randomUUID(),
                        type = SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER,
                        arbeidssituasjon = it,
                    )
                }

        (soeknaderSomSkalLagres + soeknaderSomIkkeSkalLagres).forEach { soeknadService.behandleSoeknad(it) }

        val lagredeSoeknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoeknader.size shouldBe 2
        lagredeSoeknader.map { it?.id }.toSet() shouldBe setOf(idSomSkalLagres1, idSomSkalLagres2)

        verify(exactly = 2) {
            dialogportenService.oppdaterDialogMedSykepengesoeknad(
                match { it.soeknadId == idSomSkalLagres1 || it.soeknadId == idSomSkalLagres2 },
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
        verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
    }

    @Test
    fun `skal _ikke_ lagre eller videresende søknad dersom statusen er noe annet enn sendt`() {
        val soeknad = soeknadMock()

        val statuserSomIkkeSkalLagres =
            SykepengesoknadDTO.SoknadsstatusDTO.entries.minus(SykepengesoknadDTO.SoknadsstatusDTO.SENDT)

        val soeknaderSomIkkeSkalLagres =
            statuserSomIkkeSkalLagres.map { soeknad.copy(id = UUID.randomUUID(), status = it) }

        soeknaderSomIkkeSkalLagres.forEach { soeknadService.behandleSoeknad(it) }

        val lagretSoeknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoeknad shouldBe null
        verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
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
        verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
    }

    @Test
    fun `skal _ikke_ lagre eller videresende søknad dersom det allerede finnes en søknad i databasen med den IDen`() {
        val soeknad = soeknadMock()
        val soeknadId = UUID.randomUUID()

        val soeknadSomSkalLagres = soeknad.medId(id = soeknadId)

        val soeknadSomIkkeSkalLagres =
            soeknad.copy(id = soeknadId, fom = soeknad.fom?.minusDays(1))

        soeknadService.behandleSoeknad(soeknadSomSkalLagres)
        soeknadService.behandleSoeknad(soeknadSomIkkeSkalLagres)

        val lagredeSoeknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoeknader.size shouldBe 1
        lagredeSoeknader.first()?.fom shouldBe soeknadSomSkalLagres.fom
        verify(exactly = 1) { dialogportenService.oppdaterDialogMedSykepengesoeknad(match { it.soeknadId == soeknadSomSkalLagres.id }) }
    }
}
