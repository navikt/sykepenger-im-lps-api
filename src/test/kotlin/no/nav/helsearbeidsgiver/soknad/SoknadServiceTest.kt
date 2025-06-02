package no.nav.helsearbeidsgiver.soknad

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soknad.SoeknadEntitet.sykepengesoeknad
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.soknadMock
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
    private lateinit var soeknadService: SoeknadService
    private lateinit var soeknadRepository: SoeknadRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        soeknadRepository = SoeknadRepository(db)
        soeknadService = SoeknadService(soeknadRepository)
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SoeknadEntitet.deleteAll() }
    }

    @Test
    fun `skal lagre søknad som skal sendes til arbeidsgiver`() {
        val soknad =
            soknadMock()

        soeknadService.behandleSoeknad(soknad)

        val lagretSoknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoknad shouldBe soknad
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den mangler sykmeldingId`() {
        val soknad =
            soknadMock().copy(sykmeldingId = null)

        soeknadService.behandleSoeknad(soknad)

        val lagretSoknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den mangler orgnr`() {
        val soknad = soknadMock()

        val soknad1 =
            soknad.copy(arbeidsgiver = null)
        val soknad2 =
            soknad.copy(arbeidsgiver = soknad.arbeidsgiver?.copy(orgnummer = null))

        soeknadService.behandleSoeknad(soknad1)
        soeknadService.behandleSoeknad(soknad2)

        val lagretSoknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

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

        soknaderSomIkkeSkalLagres.forEach { soeknadService.behandleSoeknad(it) }

        val lagretSoknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

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

        (soknaderSomSkalLagres + soknaderSomIkkeSkalLagres).forEach { soeknadService.behandleSoeknad(it) }

        val lagredeSoknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoknader.size shouldBe 2
        lagredeSoknader.map { it?.id }.toSet() shouldBe setOf(idSomSkalLagres1, idSomSkalLagres2)
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom den ettersendt til Nav`() {
        val soknad = soknadMock()

        val soknadSomIkkeSkalLagres =
            soknad.copy(sendtArbeidsgiver = LocalDateTime.now().minusDays(1), sendtNav = LocalDateTime.now())

        soeknadService.behandleSoeknad(soknadSomIkkeSkalLagres)

        val lagretSoknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom statusen er noe annet enn sendt`() {
        val soknad = soknadMock()

        val statuserSomIkkeSkalLagres =
            SykepengesoknadDTO.SoknadsstatusDTO.entries.minus(SykepengesoknadDTO.SoknadsstatusDTO.SENDT)

        val soknaderSomIkkeSkalLagres =
            statuserSomIkkeSkalLagres.map { soknad.copy(id = UUID.randomUUID(), status = it) }

        soknaderSomIkkeSkalLagres.forEach { soeknadService.behandleSoeknad(it) }

        val lagretSoknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom feltet sendtArbeidsgiver er null`() {
        val soknad = soknadMock()

        val soknadSomIkkeSkalLagres =
            soknad.copy(id = UUID.randomUUID(), sendtArbeidsgiver = null)

        soeknadService.behandleSoeknad(soknadSomIkkeSkalLagres)

        val lagretSoknad =
            transaction(db) { SoeknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoeknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `skal _ikke_ lagre søknad dersom det allerede finnes en søknad i databasen med den IDen`() {
        val soknad = soknadMock()
        val soknadId = UUID.randomUUID()

        val soknadSomSkalLagres = soknad.copy(id = soknadId)

        val soknadSomIkkeSkalLagres =
            soknad.copy(id = soknadId, fom = soknad.fom?.minusDays(1))

        soeknadService.behandleSoeknad(soknadSomSkalLagres)
        soeknadService.behandleSoeknad(soknadSomIkkeSkalLagres)

        val lagredeSoknader =
            transaction(db) { SoeknadEntitet.selectAll().map { it.getOrNull(sykepengesoeknad) } }

        lagredeSoknader.size shouldBe 1
        lagredeSoknader.first()?.fom shouldBe soknadSomSkalLagres.fom
    }
}
