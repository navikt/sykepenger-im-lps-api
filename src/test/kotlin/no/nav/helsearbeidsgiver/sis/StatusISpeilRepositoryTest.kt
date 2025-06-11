package no.nav.helsearbeidsgiver.sis

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.kafka.sis.Behandlingstatusmelding
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.OffsetDateTime
import java.util.UUID

@WithPostgresContainer
@ExtendWith(TransactionalExtension::class)
class StatusISpeilRepositoryTest {
    private lateinit var db: Database
    private lateinit var statusISpeilRepo: StatusISpeilRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        statusISpeilRepo = configureRepositories(db).statusISpeilRepository
    }

    @Test
    fun `lagreStatus`() {
        val soeknadId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingstatusmelding =
            Behandlingstatusmelding(
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = UUID.randomUUID(),
                tidspunkt = OffsetDateTime.now(),
                status = Behandlingstatusmelding.Behandlingstatustype.OPPRETTET,
                eksterneSøknadIder = setOf(soeknadId),
            )
        statusISpeilRepo.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding)
        statusISpeilRepo.hentSoeknaderForVedtaksperiodeId(vedtaksperiodeId) shouldContainExactly setOf(soeknadId)
    }

    @Test
    fun `hindre duplikater i databasen`() {
        val soeknadId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingstatusmelding =
            Behandlingstatusmelding(
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = UUID.randomUUID(),
                tidspunkt = OffsetDateTime.now(),
                status = Behandlingstatusmelding.Behandlingstatustype.OPPRETTET,
                eksterneSøknadIder = setOf(soeknadId),
            )
        statusISpeilRepo.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding)
        statusISpeilRepo.hentSoeknaderForVedtaksperiodeId(vedtaksperiodeId) shouldContainExactly setOf(soeknadId)
        shouldNotThrowAny {
            statusISpeilRepo.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding)
        }
        statusISpeilRepo.hentSoeknaderForVedtaksperiodeId(vedtaksperiodeId) shouldContainExactly setOf(soeknadId)
    }

    @Test
    fun `Støtter oppdaterte søknader i databasen`() {
        val soeknadId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingstatusmelding =
            Behandlingstatusmelding(
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = UUID.randomUUID(),
                tidspunkt = OffsetDateTime.now(),
                status = Behandlingstatusmelding.Behandlingstatustype.OPPRETTET,
                eksterneSøknadIder = setOf(soeknadId),
            )
        val soeknadId2 = UUID.randomUUID()
        statusISpeilRepo.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding)
        statusISpeilRepo.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding.copy(eksterneSøknadIder = setOf(soeknadId, soeknadId2)))
        statusISpeilRepo.hentSoeknaderForVedtaksperiodeId(vedtaksperiodeId) shouldContainExactly setOf(soeknadId, soeknadId2)
    }
}
