package no.nav.helsearbeidsgiver.soeknad

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.kafka.sis.Behandlingstatusmelding
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.sis.StatusISpeilRepository
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet.soeknadId
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.medId
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
import java.time.OffsetDateTime
import java.util.UUID

@WithPostgresContainer
class SoeknadRepositoryTest {
    private lateinit var db: Database
    private lateinit var soeknadRepository: SoeknadRepository
    private lateinit var statusISpeilRepository: StatusISpeilRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        soeknadRepository = SoeknadRepository(db)
        statusISpeilRepository = StatusISpeilRepository(db)
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

        val resultatFoer = hentSoeknader(setOf(soeknad.id))
        resultatFoer[soeknad.id] shouldBe null

        soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(setOf(soeknad.id), vedtaksperiodeId)

        val resultatEtter = hentSoeknader(setOf(soeknad.id))
        resultatEtter[soeknad.id] shouldBe vedtaksperiodeId
    }

    @Test
    fun `oppdaterSoeknaderMedVedtaksperiodeId skal lagre vedtaksperiodeId på flere søknader`() {
        val soeknad = soeknadMock()
        val soeknad2 = soeknadMock().medId(id = UUID.randomUUID())
        val soeknad3 = soeknadMock().medId(id = UUID.randomUUID())
        val vedtaksperiodeId = UUID.randomUUID()

        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad2.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad3.tilLagreSoeknad())

        val soeknadTilVedtaksperiodeIdMapFoer = hentSoeknader(setOf(soeknad.id, soeknad2.id, soeknad3.id))

        soeknadTilVedtaksperiodeIdMapFoer[soeknad.id] shouldBe null
        soeknadTilVedtaksperiodeIdMapFoer[soeknad2.id] shouldBe null
        soeknadTilVedtaksperiodeIdMapFoer[soeknad3.id] shouldBe null

        soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(setOf(soeknad.id, soeknad3.id), vedtaksperiodeId)

        val soeknadTilVedtaksperiodeIdMap = hentSoeknader(setOf(soeknad.id, soeknad2.id, soeknad3.id))
        soeknadTilVedtaksperiodeIdMap[soeknad.id] shouldBe vedtaksperiodeId
        soeknadTilVedtaksperiodeIdMap[soeknad2.id] shouldBe null
        soeknadTilVedtaksperiodeIdMap[soeknad3.id] shouldBe vedtaksperiodeId
    }

    @Test
    fun `oppdaterSoeknaderMedVedtaksperiodeId skal bare oppdatere søknader som mangler vedtaksperiodeId`() {
        val soeknad = soeknadMock()
        val soeknad2 = soeknadMock().medId(id = UUID.randomUUID())
        val soeknad3 = soeknadMock().medId(id = UUID.randomUUID())
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad2.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad3.tilLagreSoeknad())
        soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(setOf(soeknad.id), vedtaksperiodeId)

        val soeknadTilVedtaksperiodeIdMapFoer = hentSoeknader(setOf(soeknad.id, soeknad2.id, soeknad3.id))

        soeknadTilVedtaksperiodeIdMapFoer[soeknad.id] shouldBe vedtaksperiodeId
        soeknadTilVedtaksperiodeIdMapFoer[soeknad2.id] shouldBe null
        soeknadTilVedtaksperiodeIdMapFoer[soeknad3.id] shouldBe null

        soeknadRepository.oppdaterSoeknaderMedVedtaksperiodeId(setOf(soeknad2.id, soeknad3.id), vedtaksperiodeId2)

        val soeknadTilVedtaksperiodeIdMap = hentSoeknader(setOf(soeknad.id, soeknad2.id, soeknad3.id))
        soeknadTilVedtaksperiodeIdMap[soeknad.id] shouldBe vedtaksperiodeId
        soeknadTilVedtaksperiodeIdMap[soeknad2.id] shouldBe vedtaksperiodeId2
        soeknadTilVedtaksperiodeIdMap[soeknad3.id] shouldBe vedtaksperiodeId2
    }

    @Test
    fun `lagreSoeknad skal lagre søknad`() {
        val soeknad = soeknadMock()

        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())

        val lagretSoeknad = soeknadRepository.hentSoeknad(soeknad.id)

        lagretSoeknad?.sykepengesoknadDTO shouldBe soeknad
    }

    @Test
    fun `hentSoeknad skal hente søknad med id`() {
        val soeknader = List(10) { UUID.randomUUID() }.map { id -> soeknadMock().medId(id = id) }

        soeknader.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }

        val soeknadValgt = soeknader[2]

        soeknadRepository.hentSoeknad(soeknadValgt.id)?.sykepengesoknadDTO shouldBe soeknadValgt
    }

    @Test
    fun `hentSoeknad takler at sendt-felter ikke er populert`() {
        val soeknad = soeknadMock().copy(sendtNav = null, sendtArbeidsgiver = null)
        soeknadRepository.lagreSoeknad(soeknad.tilLagreSoeknad())
        soeknadRepository.hentSoeknad(soeknad.id)?.sykepengesoknadDTO shouldBe soeknad
    }

    @Test
    fun `hentSoeknader skal bare hente søknader med riktig orgnr`() {
        val orgnr = Orgnr.genererGyldig()
        val soeknaderMedSammeOrgnr =
            List(3) { UUID.randomUUID() }.map { id ->
                soeknadMock().copy(
                    id = id,
                    arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Testorganisasjon", orgnr.verdi),
                )
            }
        val soeknader =
            List(5) { UUID.randomUUID() }.map { id ->
                soeknadMock().copy(
                    id = id,
                    arbeidsgiver =
                        SykepengesoknadDTO.ArbeidsgiverDTO(
                            "Tilfeldig Tigerorg",
                            Orgnr.genererGyldig().verdi,
                        ),
                )
            }
        soeknader.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }
        soeknaderMedSammeOrgnr.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }
        val soeknaderFraRepo =
            soeknadRepository.hentSoeknader(filter = SykepengesoeknadFilter(orgnr = orgnr.verdi)).map { it.sykepengesoknadDTO }

        soeknaderFraRepo shouldContainOnly soeknaderMedSammeOrgnr
    }

    @Test
    fun `hentSoeknaderMedVedtaksperiodeId skal bare hente søknader som også finnes i status-i-speil-tabell`() {
        val soeknader =
            List(5) { UUID.randomUUID() }.map { id ->
                soeknadMock().copy(
                    id = id,
                    arbeidsgiver =
                        SykepengesoknadDTO.ArbeidsgiverDTO(
                            "Tilfeldig Tigerorg",
                            Orgnr.genererGyldig().verdi,
                        ),
                )
            }
        soeknader.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }

        val vedtaksperiodeId = UUID.randomUUID()
        soeknadRepository.hentSoeknaderMedVedtaksperiodeId(vedtaksperiodeId) shouldBe emptyList()

        val soeknaderMedVedtaksperiodeId = soeknader.take(2)
        statusISpeilRepository.lagreNyeSoeknaderOgStatuser(
            Behandlingstatusmelding(
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = UUID.randomUUID(),
                tidspunkt = OffsetDateTime.now(),
                status = Behandlingstatusmelding.Behandlingstatustype.OPPRETTET,
                eksterneSøknadIder = soeknaderMedVedtaksperiodeId.map { it.id }.toSet(),
            ),
        )

        soeknadRepository.hentSoeknaderMedVedtaksperiodeId(vedtaksperiodeId) shouldBe soeknaderMedVedtaksperiodeId
    }

    @Test
    fun `repository skal begrense antall entiteter som returneres - kan max returnere maxLimit + 1`() {
        val orgnr = Orgnr.genererGyldig()
        val soeknaderMedSammeOrgnr =
            List(MAX_ANTALL_I_RESPONS + 10) { UUID.randomUUID() }.map { id ->
                soeknadMock().copy(
                    id = id,
                    arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Testorganisasjon", orgnr.verdi),
                )
            }
        soeknaderMedSammeOrgnr.forEach { soeknadRepository.lagreSoeknad(it.tilLagreSoeknad()) }
        soeknadRepository.hentSoeknader(SykepengesoeknadFilter(orgnr.verdi)).size shouldBe MAX_ANTALL_I_RESPONS + 1
    }

    @Test
    fun `hentSoeknader fraLoepenr skal returnere kun loepenr større enn oppgitt verdi`() {
        val orgnr = Orgnr.genererGyldig()
        val soeknad1 =
            soeknadMock().copy(
                id = UUID.randomUUID(),
                arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Testorganisasjon", orgnr.verdi),
            )
        val soeknad2 =
            soeknadMock().copy(
                id = UUID.randomUUID(),
                arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Testorganisasjon", orgnr.verdi),
            )
        val soeknad3 =
            soeknadMock().copy(
                id = UUID.randomUUID(),
                arbeidsgiver = SykepengesoknadDTO.ArbeidsgiverDTO("Testorganisasjon", orgnr.verdi),
            )
        soeknadRepository.lagreSoeknad(soeknad1.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad2.tilLagreSoeknad())
        soeknadRepository.lagreSoeknad(soeknad3.tilLagreSoeknad())
        val soeknad1LopeNr =
            soeknadRepository
                .hentSoeknader(SykepengesoeknadFilter(orgnr = orgnr.verdi))
                .first { it.sykepengesoknadDTO.id == soeknad1.id }
                .loepenr
        soeknadRepository.hentSoeknader(SykepengesoeknadFilter(orgnr = orgnr.verdi, fraLoepenr = soeknad1LopeNr)).size shouldBe 2
    }

    private fun hentSoeknader(soeknadIder: Set<UUID>): Map<UUID, UUID?> =
        transaction(db) {
            SoeknadEntitet
                .selectAll()
                .where {
                    soeknadId inList soeknadIder
                }.toList()
                .associate { it[soeknadId] to it[SoeknadEntitet.vedtaksperiodeId] }
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
