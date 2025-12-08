package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.buildForespoerselOppdatertJson
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

@WithPostgresContainer
class ForepoerselBacklogTest {
    private lateinit var db: Database
    private lateinit var forespoerselRepository: ForespoerselRepository
    private lateinit var forespoerselService: ForespoerselService

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        forespoerselRepository = configureRepositories(db).forespoerselRepository
        forespoerselService = ForespoerselService(forespoerselRepository, dialogportenService = mockk(), dokumentkoblinService = mockk())
    }

    @Test
    fun `lagre eller oppdatere forespoersel test`() {
        val forespoerselId = UUID.randomUUID()
        val eksponertForespoerselId = UUID.randomUUID()
        val priMessage =
            jsonMapper.decodeFromString<PriMessage>(
                buildForespoerselOppdatertJson(forespoerselId = forespoerselId, eksponertForespoerselId = eksponertForespoerselId),
            )
        priMessage.forespoersel?.let {
            forespoerselRepository.lagreForespoersel(it, eksponertForespoerselId = forespoerselId)
        }

        forespoerselService.hentEksponertForespoerselId(forespoerselId) shouldBe forespoerselId

        priMessage.forespoersel?.let { forespoersel ->
            priMessage.eksponertForespoerselId?.let { eksponertFsp ->
                forespoerselService.lagreEllerOppdaterForespoersel(
                    forespoersel = forespoersel,
                    status = priMessage.status,
                    eksponertForespoerselId = eksponertFsp,
                )
            }
        }

        forespoerselService.hentEksponertForespoerselId(forespoerselId).let {
            it shouldNotBe null
            it shouldBe eksponertForespoerselId
        }
    }
}
