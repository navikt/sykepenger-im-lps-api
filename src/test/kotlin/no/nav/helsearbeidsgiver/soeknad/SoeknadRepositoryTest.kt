package no.nav.helsearbeidsgiver.soeknad

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.soeknadId
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@WithPostgresContainer
class SoeknadRepositoryTest {
    private lateinit var db: Database
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
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SoeknadEntitet.deleteAll() }
    }

    @Test
    fun `oppdaterSoeknaderMedVedtaksperiodeId skal lagre vedtaksperiodeId på en søknad`() {
        val soeknad = soeknadMock()
        val vedtaksperiodeId = UUID.randomUUID()

        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())

        val resultatFør =
            transaction(db) {
                SoeknadEntitet
                    .selectAll()
                    .where {
                        soeknadId eq soeknad.id
                    }.firstOrNull()
            }
        resultatFør?.getOrNull(SoeknadEntitet.vedtaksperiodeId) shouldBe null

        soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(setOf(soeknad.id), vedtaksperiodeId)

        val resultatEtter =
            transaction(db) {
                SoeknadEntitet
                    .selectAll()
                    .where {
                        soeknadId eq soeknad.id
                    }.firstOrNull()
            }
        resultatEtter?.getOrNull(SoeknadEntitet.vedtaksperiodeId) shouldBe vedtaksperiodeId
    }

    @Test
    fun `oppdaterSoeknaderMedVedtaksperiodeId skal lagre vedtaksperiodeId på flere søknader`() {
        val soeknad = soeknadMock()
        val soeknad2 = soeknadMock().copy(id = UUID.randomUUID())
        val soeknad3 = soeknadMock().copy(id = UUID.randomUUID())
        val vedtaksperiodeId = UUID.randomUUID()

        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad2.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad3.tilLagreSoeknad())

        val resultatFør =
            transaction(db) {
                SoeknadEntitet
                    .selectAll()
                    .where {
                        soeknadId inList listOf(soeknad.id, soeknad2.id, soeknad3.id)
                    }.toList()
            }
        val soeknadTilVedtaksperiodeIdMapFør = resultatFør.associate { it[soeknadId] to it[SoeknadEntitet.vedtaksperiodeId] }

        soeknadTilVedtaksperiodeIdMapFør[soeknad.id] shouldBe null
        soeknadTilVedtaksperiodeIdMapFør[soeknad2.id] shouldBe null
        soeknadTilVedtaksperiodeIdMapFør[soeknad3.id] shouldBe null

        soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(setOf(soeknad.id, soeknad3.id), vedtaksperiodeId)

        val resultatEtter =
            transaction(db) {
                SoeknadEntitet
                    .selectAll()
                    .where {
                        soeknadId inList listOf(soeknad.id, soeknad2.id, soeknad3.id)
                    }.toList()
            }
        val soeknadTilVedtaksperiodeIdMap = resultatEtter.associate { it[soeknadId] to it[SoeknadEntitet.vedtaksperiodeId] }
        soeknadTilVedtaksperiodeIdMap[soeknad.id] shouldBe vedtaksperiodeId
        soeknadTilVedtaksperiodeIdMap[soeknad2.id] shouldBe null
        soeknadTilVedtaksperiodeIdMap[soeknad3.id] shouldBe vedtaksperiodeId
    }

    @Test
    fun `lagreSoeknad skal lagre søknad`() {
        val soeknad = soeknadMock()

        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())

        val lagretSoeknad = soeknadRepository.hentSoeknad(soeknad.id)

        lagretSoeknad shouldBe soeknad
    }

    @Test
    fun `hentSoeknad skal hente søknad med id`() {
        val soeknader = List(10) { UUID.randomUUID() }.map { id -> soeknadMock().copy(id = id) }

        soeknader.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }

        val soeknadValgt = soeknader[2]

        soeknadRepository.hentSoeknad(soeknadValgt.id) shouldBe soeknadValgt
    }

    @Test
    fun `hentSoeknader skal bare hente søknader med riktig orgnr`() {
        val orgnr = Orgnr.genererGyldig()
        val soeknaderMedSammeOrgnr =
            List(3) { UUID.randomUUID() }.map { id ->
                soeknadMock().copy(id = id, arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Test organisasjon", orgnr.verdi))
            }
        val soeknader =
            List(5) { UUID.randomUUID() }.map { id ->
                soeknadMock().copy(
                    id = id,
                    arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Tilfeltdig Tigerorg", Orgnr.genererGyldig().verdi),
                )
            }
        soeknader.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }
        soeknaderMedSammeOrgnr.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }
        soeknadRepository.hentSoeknader(orgnr.verdi) shouldContainOnly soeknaderMedSammeOrgnr
    }

    private fun SykepengesoknadDTO.tilLagreSoeknad(): LagreSoeknad =
        LagreSoeknad(
            soeknadId = id,
            sykmeldingId = sykmeldingId!!,
            fnr = fnr,
            orgnr = arbeidsgiver?.orgnummer!!,
            sykepengesoeknad = this,
        )
}
