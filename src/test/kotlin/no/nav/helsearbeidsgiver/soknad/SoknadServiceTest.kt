package no.nav.helsearbeidsgiver.soknad

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.dialogporten.DialogSykepengesoknad
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soknad.SoknadEntitet.sykepengesoknad
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.soknadMock
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
class SoknadServiceTest {
    private lateinit var db: Database
    private lateinit var soknadService: SoknadService
    private lateinit var soknadRepository: SoknadRepository
    private lateinit var dialogportenService: DialogportenService

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        soknadRepository = SoknadRepository(db)
        dialogportenService = mockk<DialogportenService>()
        soknadService = SoknadService(soknadRepository, dialogportenService)

        every { dialogportenService.oppdaterDialogMedSykepengesoknad(any()) } just Runs
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SoknadEntitet.deleteAll() }
    }

    @Test
    fun `skal lagre søknad som skal sendes til arbeidsgiver`() {
        val soknad =
            soknadMock()

        soknadService.behandleSoknad(soknad)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe soknad
    }

    @Test
    fun `søknad som skal sendes til arbeidsgiver sendes videre til hag-dialog`() {
        val soknad =
            soknadMock()

        soknadService.behandleSoknad(soknad)

        val forventetDialogSykepengesoknad =
            DialogSykepengesoknad(
                soknadId = soknad.id,
                sykmeldingId = soknad.sykmeldingId!!,
                orgnr = Orgnr(soknad.arbeidsgiver!!.orgnummer!!),
            )
        verify(exactly = 1) { dialogportenService.oppdaterDialogMedSykepengesoknad(forventetDialogSykepengesoknad) }
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den mangler sykmeldingId`() {
        val soknad =
            soknadMock().copy(sykmeldingId = null)

        soknadService.behandleSoknad(soknad)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den mangler orgnr`() {
        val soknad = soknadMock()

        val soknad1 =
            soknad.copy(arbeidsgiver = null)
        val soknad2 =
            soknad.copy(arbeidsgiver = soknad.arbeidsgiver?.copy(orgnummer = null))

        soknadService.behandleSoknad(soknad1)
        soknadService.behandleSoknad(soknad2)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den er en søknadstype som ikke skal sendes til arbeidsgiver`() {
        val soknad = soknadMock()

        val soknadstyperSomIkkeSkalLagres =
            SykepengesoknadDTO.SoknadstypeDTO.entries
                .minus(
                    listOf(
                        SykepengesoknadDTO.SoknadstypeDTO.ARBEIDSTAKERE,
                        SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                        SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER,
                    ),
                )

        val soknaderSomIkkeSkalLagres = soknadstyperSomIkkeSkalLagres.map { soknad.copy(type = it) }

        soknaderSomIkkeSkalLagres.forEach { soknadService.behandleSoknad(it) }

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad hvis søknadstype GRADERT_REISETILSKUDD eller BEHANDLINGSDAGER, men ikke arbeidssituasjon ARBEIDSTAKER`() {
        val soknad = soknadMock()

        val idSomSkalLagres1 = UUID.randomUUID()
        val idSomSkalLagres2 = UUID.randomUUID()

        val soknaderSomSkalLagres =
            listOf(
                soknad.copy(
                    id = idSomSkalLagres1,
                    type = SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                    arbeidssituasjon = SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER,
                ),
                soknad.copy(
                    id = idSomSkalLagres2,
                    type = SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER,
                    arbeidssituasjon = SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER,
                ),
            )

        val arbeidssituasjonerSomIkkeSkalLagres =
            SykepengesoknadDTO.ArbeidssituasjonDTO.entries
                .minus(SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER)

        val soknaderSomIkkeSkalLagres =
            arbeidssituasjonerSomIkkeSkalLagres.map {
                soknad.copy(
                    id = UUID.randomUUID(),
                    type = SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD,
                    arbeidssituasjon = it,
                )
            } +
                arbeidssituasjonerSomIkkeSkalLagres.map {
                    soknad.copy(
                        id = UUID.randomUUID(),
                        type = SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER,
                        arbeidssituasjon = it,
                    )
                }

        (soknaderSomSkalLagres + soknaderSomIkkeSkalLagres).forEach { soknadService.behandleSoknad(it) }

        val lagredeSoknader =
            transaction(db) { SoknadEntitet.selectAll().map { it.getOrNull(sykepengesoknad) } }

        lagredeSoknader.size shouldBe 2
        lagredeSoknader.map { it?.id }.toSet() shouldBe setOf(idSomSkalLagres1, idSomSkalLagres2)
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den ettersendt til Nav`() {
        val soknad = soknadMock()

        val soknadSomIkkeSkalLagres =
            soknad.copy(sendtArbeidsgiver = LocalDateTime.now().minusDays(1), sendtNav = LocalDateTime.now())

        soknadService.behandleSoknad(soknadSomIkkeSkalLagres)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom statusen er noe annet enn sendt`() {
        val soknad = soknadMock()

        val statuserSomIkkeSkalLagres =
            SykepengesoknadDTO.SoknadsstatusDTO.entries.minus(SykepengesoknadDTO.SoknadsstatusDTO.SENDT)

        val soknaderSomIkkeSkalLagres =
            statuserSomIkkeSkalLagres.map { soknad.copy(id = UUID.randomUUID(), status = it) }

        soknaderSomIkkeSkalLagres.forEach { soknadService.behandleSoknad(it) }

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom feltet sendtArbeidsgiver er null`() {
        val soknad = soknadMock()

        val soknadSomIkkeSkalLagres =
            soknad.copy(id = UUID.randomUUID(), sendtArbeidsgiver = null)

        soknadService.behandleSoknad(soknadSomIkkeSkalLagres)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom det allerede finnes en søknad i databasen med den IDen`() {
        val soknad = soknadMock()
        val soknadId = UUID.randomUUID()

        val soknadSomSkalLagres = soknad.copy(id = soknadId)

        val soknadSomIkkeSkalLagres =
            soknad.copy(id = soknadId, fom = soknad.fom?.minusDays(1))

        soknadService.behandleSoknad(soknadSomSkalLagres)
        soknadService.behandleSoknad(soknadSomIkkeSkalLagres)

        val lagredeSoknader =
            transaction(db) { SoknadEntitet.selectAll().map { it.getOrNull(sykepengesoknad) } }

        lagredeSoknader.size shouldBe 1
        lagredeSoknader.first()?.fom shouldBe soknadSomSkalLagres.fom
    }
}
