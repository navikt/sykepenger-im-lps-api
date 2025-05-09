package no.nav.helsearbeidsgiver.soknad

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.soknad.SoknadEntitet.sykepengesoknad
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.soknadMock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@WithPostgresContainer
class SoknadServiceTest {
    private lateinit var db: Database
    private lateinit var soknadService: SoknadService
    private lateinit var soknadRepository: SoknadRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        soknadRepository = SoknadRepository(db)
        soknadService = SoknadService(soknadRepository)
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SoknadEntitet.deleteAll() }
    }

    @Test
    fun `lagreSoknad skal lagre søknad`() {
        val soknad =
            soknadMock()

        soknadService.lagreSoknad(soknad)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe soknad
    }

    @Test
    fun `lagreSoknad skal _ikke_ lagre søknad dersom den mangler sykmeldingId`() {
        val soknad =
            soknadMock().copy(sykmeldingId = null)

        soknadService.lagreSoknad(soknad)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }

    @Test
    fun `lagreSoknad skal _ikke_ lagre søknad dersom den mangler orgnr`() {
        val soknad = soknadMock()

        val soknad1 =
            soknad.copy(arbeidsgiver = null)
        val soknad2 =
            soknad.copy(arbeidsgiver = soknad.arbeidsgiver?.copy(orgnummer = null))

        soknadService.lagreSoknad(soknad1)
        soknadService.lagreSoknad(soknad2)

        val lagretSoknad =
            transaction(db) { SoknadEntitet.selectAll().firstOrNull()?.getOrNull(sykepengesoknad) }

        lagretSoknad shouldBe null
    }
}
