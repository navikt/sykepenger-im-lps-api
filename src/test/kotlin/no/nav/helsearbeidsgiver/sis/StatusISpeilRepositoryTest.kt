package no.nav.helsearbeidsgiver.sis

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.kafka.sis.Behandlingstatusmelding
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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
        val behandlingstatusmelding =
            Behandlingstatusmelding(
                vedtaksperiodeId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                tidspunkt = OffsetDateTime.now(),
                status = Behandlingstatusmelding.Behandlingstatustype.OPPRETTET,
                eksterneSøknadIder = setOf(UUID.randomUUID()),
            )
        statusISpeilRepo.lagreNyeSoeknaderOgStatuser(behandlingstatusmelding)
        val resultat: Map<UUID, UUID> =
            transaction(db) {
                StatusISpeilEntitet
                    .selectAll()
                    .associate { it[StatusISpeilEntitet.vedtaksperiodeId] to it[StatusISpeilEntitet.soeknadId] }
            }
        resultat[behandlingstatusmelding.vedtaksperiodeId] shouldBe behandlingstatusmelding.eksterneSøknadIder.first()
    }
}
