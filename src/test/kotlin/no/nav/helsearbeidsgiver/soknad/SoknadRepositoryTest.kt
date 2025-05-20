package no.nav.helsearbeidsgiver.soknad

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.soknadMock
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
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

        soknadRepository.lagreSoknad(soknad.tilLagreSoknad())

        val lagretSoknad = soknadRepository.hentSoknad(soknad.id)

        lagretSoknad shouldBe soknad
    }

    @Test
    fun `hentSoknad skal hente søknad med id`() {
        val soknader = List(10) { UUID.randomUUID() }.map { id -> soknadMock().copy(id = id) }

        soknader.forEach { soknadRepository.lagreSoknad(it.tilLagreSoknad()) }

        val soknadValgt = soknader[2]

        soknadRepository.hentSoknad(soknadValgt.id) shouldBe soknadValgt
    }

    @Test
    fun `hentSoknader skal bare hente søknader med riktig orgnr`() {
        val orgnr = Orgnr.genererGyldig()
        val soknaderMedSammeOrgnr =
            List(3) { UUID.randomUUID() }.map { id ->
                soknadMock().copy(id = id, arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Test organisasjon", orgnr.verdi))
            }
        val soknader =
            List(5) { UUID.randomUUID() }.map { id ->
                soknadMock().copy(
                    id = id,
                    arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Tilfeltdig Tigerorg", Orgnr.genererGyldig().verdi),
                )
            }
        soknader.forEach { soknadRepository.lagreSoknad(it.tilLagreSoknad()) }
        soknaderMedSammeOrgnr.forEach { soknadRepository.lagreSoknad(it.tilLagreSoknad()) }
        soknadRepository.hentSoknader(orgnr.verdi) shouldContainOnly soknaderMedSammeOrgnr
    }

    private fun SykepengesoknadDTO.tilLagreSoknad(): LagreSoknad =
        LagreSoknad(
            soknadId = id,
            sykmeldingId = sykmeldingId!!,
            fnr = fnr,
            orgnr = arbeidsgiver?.orgnummer!!,
            sykepengesoknad = this,
        )
}
