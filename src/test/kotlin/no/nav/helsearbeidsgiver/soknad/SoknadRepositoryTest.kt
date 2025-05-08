package no.nav.helsearbeidsgiver.soknad

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.soknadMock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@WithPostgresContainer
class SoknadRepositoryTest {
    private lateinit var db: Database
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
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SoknadEntitet.deleteAll() }
    }

    @Test
    fun `lagreSoknad skal lagre søknad`() {
        val soknad = soknadMock()

        soknadRepository.lagreSoknad(soknad)

        val lagretSoknad = soknadRepository.hentSoknad(soknad.id)

        lagretSoknad shouldBe soknad
    }

    @Test
    fun `lagreSoknad skal ignorere søknader som mangler sykmeldingId`() {
        val soknad = soknadMock().copy(sykmeldingId = null)

        soknadRepository.lagreSoknad(soknad)

        val lagretSoknad = soknadRepository.hentSoknad(soknad.id)

        lagretSoknad shouldBe null
    }

    @Test
    fun `lagreSoknad skal ignorere søknader som mangler orgnr`() {
        val arbeidsgiver = soknadMock().arbeidsgiver!!.copy(orgnummer = null)
        val soknad = soknadMock().copy(arbeidsgiver = arbeidsgiver)

        soknadRepository.lagreSoknad(soknad)

        val lagretSoknad = soknadRepository.hentSoknad(soknad.id)

        lagretSoknad shouldBe null
    }

    @Test
    fun `hentSoknad skal hente søknad med id`() {
        val soknader = List(10) { UUID.randomUUID() }.map { id -> soknadMock().copy(id = id) }

        soknader.forEach { soknadRepository.lagreSoknad(it) }

        val soknadValgt = soknader[2]

        soknadRepository.hentSoknad(soknadValgt.id) shouldBe soknadValgt
    }
}
