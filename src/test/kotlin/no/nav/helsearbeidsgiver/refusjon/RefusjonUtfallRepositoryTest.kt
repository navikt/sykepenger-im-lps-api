package no.nav.helsearbeidsgiver.refusjon

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@WithPostgresContainer
class RefusjonUtfallRepositoryTest {
    private val db: Database by lazy {
        DatabaseConfig(
            System.getProperty("database.url"),
            System.getProperty("database.username"),
            System.getProperty("database.password"),
        ).init()
    }
    private val refusjonUtfallRepository: RefusjonUtfallRepository by lazy { RefusjonUtfallRepository(db) }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { RefusjonUtfallEntitet.deleteAll() }
    }

    @Test
    fun `lagreRefusjonUtfall skal lagre refusjonUtfallId, vedtaksperiodeId og orgnr i egne kolonner og hele meldingen som jsonb`() {
        val refusjonUtfall = refusjonUtfallMock()

        val bleLagret = refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)

        bleLagret shouldBe true
        val lagredeRader = hentRader(refusjonUtfall.refusjonUtfallId)
        lagredeRader shouldHaveSize 1
        lagredeRader.single()[RefusjonUtfallEntitet.refusjonUtfallId] shouldBe refusjonUtfall.refusjonUtfallId
        lagredeRader.single()[RefusjonUtfallEntitet.vedtaksperiodeId] shouldBe refusjonUtfall.vedtaksperiodeId
        lagredeRader.single()[RefusjonUtfallEntitet.orgnr] shouldBe refusjonUtfall.orgnr.toString()
        lagredeRader.single()[RefusjonUtfallEntitet.refusjonUtfall] shouldBe refusjonUtfall
    }

    @Test
    fun `lagreRefusjonUtfall skal ignorere duplikat med samme refusjonUtfallId`() {
        val refusjonUtfall = refusjonUtfallMock()
        val duplikat = refusjonUtfall.copy(sykepengegrunnlag = refusjonUtfall.sykepengegrunnlag + 1000.0)

        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall) shouldBe true
        refusjonUtfallRepository.lagreRefusjonUtfall(duplikat) shouldBe false

        val lagredeRader = hentRader(refusjonUtfall.refusjonUtfallId)
        lagredeRader shouldHaveSize 1
        lagredeRader.single()[RefusjonUtfallEntitet.refusjonUtfall] shouldBe refusjonUtfall
    }

    @Test
    fun `lagreRefusjonUtfall skal lagre flere refusjonsutfall med ulik refusjonUtfallId`() {
        val refusjonUtfall = refusjonUtfallMock()
        val annetRefusjonUtfall = refusjonUtfall.copy(refusjonUtfallId = UUID.randomUUID())

        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall) shouldBe true
        refusjonUtfallRepository.lagreRefusjonUtfall(annetRefusjonUtfall) shouldBe true

        transaction(db) { RefusjonUtfallEntitet.selectAll().count() } shouldBe 2
    }

    @Test
    fun `hentRefusjonUtfall skal hente refusjonsutfall med loepenr`() {
        val refusjonUtfall = refusjonUtfallMock()
        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        val forventetLoepenr = hentRader(refusjonUtfall.refusjonUtfallId).single()[RefusjonUtfallEntitet.id]

        val lagretRefusjonUtfall = refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfall.refusjonUtfallId)

        lagretRefusjonUtfall?.loepenr shouldBe forventetLoepenr
        lagretRefusjonUtfall?.refusjonUtfall shouldBe refusjonUtfall
    }

    @Test
    fun `hentRefusjonUtfall skal returnere null naar refusjonsutfallet ikke finnes`() {
        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfallMock())

        refusjonUtfallRepository.hentRefusjonUtfall(UUID.randomUUID()) shouldBe null
    }

    private fun hentRader(refusjonUtfallId: UUID) =
        transaction(db) {
            RefusjonUtfallEntitet
                .selectAll()
                .where { RefusjonUtfallEntitet.refusjonUtfallId eq refusjonUtfallId }
                .toList()
        }
}
