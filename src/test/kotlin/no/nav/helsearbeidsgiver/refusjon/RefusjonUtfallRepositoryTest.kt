package no.nav.helsearbeidsgiver.refusjon

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.helsearbeidsgiver.vedtak.VedtakFilter
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
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
    fun `lagreRefusjonUtfall skal lagre refusjonUtfallId, vedtaksperiodeId, fnr og orgnr i egne kolonner og hele meldingen som jsonb`() {
        val refusjonUtfall = refusjonUtfallMock()

        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)

        val lagredeRader = hentRader(refusjonUtfall.refusjonUtfallId)
        lagredeRader shouldHaveSize 1
        lagredeRader.single()[RefusjonUtfallEntitet.refusjonUtfallId] shouldBe refusjonUtfall.refusjonUtfallId
        lagredeRader.single()[RefusjonUtfallEntitet.vedtaksperiodeId] shouldBe refusjonUtfall.vedtaksperiodeId
        lagredeRader.single()[RefusjonUtfallEntitet.fnr] shouldBe refusjonUtfall.fnr.toString()
        lagredeRader.single()[RefusjonUtfallEntitet.orgnr] shouldBe refusjonUtfall.orgnr.toString()
        lagredeRader.single()[RefusjonUtfallEntitet.refusjonUtfall] shouldBe refusjonUtfall
    }

    @Test
    fun `lagreRefusjonUtfall skal kaste feil og ikke overskrive ved duplikat refusjonUtfallId`() {
        val refusjonUtfall = refusjonUtfallMock()
        val duplikat = refusjonUtfall.copy(sykepengegrunnlag = refusjonUtfall.sykepengegrunnlag + 1000.0)

        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        assertThrows<ExposedSQLException> { refusjonUtfallRepository.lagreRefusjonUtfall(duplikat) }

        val lagredeRader = hentRader(refusjonUtfall.refusjonUtfallId)
        lagredeRader shouldHaveSize 1
        lagredeRader.single()[RefusjonUtfallEntitet.refusjonUtfall] shouldBe refusjonUtfall
    }

    @Test
    fun `lagreRefusjonUtfall skal lagre flere refusjonsutfall med ulik refusjonUtfallId`() {
        val refusjonUtfall = refusjonUtfallMock()
        val annetRefusjonUtfall = refusjonUtfall.copy(refusjonUtfallId = UUID.randomUUID())

        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        refusjonUtfallRepository.lagreRefusjonUtfall(annetRefusjonUtfall)

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

    @Test
    fun `hentRefusjonUtfall med filter skal kun hente refusjonsutfall for oppgitt orgnr`() {
        val refusjonUtfall = refusjonUtfallMock()
        val annetOrgnr = refusjonUtfallMock().copy(orgnr = Orgnr("810007842"))
        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        refusjonUtfallRepository.lagreRefusjonUtfall(annetOrgnr)

        val resultat = refusjonUtfallRepository.hentRefusjonUtfall(VedtakFilter(orgnr = refusjonUtfall.orgnr.toString()))

        resultat.map { it.refusjonUtfall } shouldBe listOf(refusjonUtfall)
    }

    @Test
    fun `hentRefusjonUtfall med filter skal filtrere paa fnr`() {
        val refusjonUtfall = refusjonUtfallMock()
        val annenSykmeldt = refusjonUtfallMock().copy(fnr = Fnr("05449412615"))
        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        refusjonUtfallRepository.lagreRefusjonUtfall(annenSykmeldt)

        val resultat =
            refusjonUtfallRepository.hentRefusjonUtfall(
                VedtakFilter(orgnr = refusjonUtfall.orgnr.toString(), fnr = refusjonUtfall.fnr.toString()),
            )

        resultat.map { it.refusjonUtfall } shouldBe listOf(refusjonUtfall)
    }

    @Test
    fun `hentRefusjonUtfall med filter skal hente refusjonsutfall etter fraLoepenr sortert på loepenr`() {
        val foerste = refusjonUtfallMock()
        val andre = refusjonUtfallMock()
        val tredje = refusjonUtfallMock()
        listOf(foerste, andre, tredje).forEach { refusjonUtfallRepository.lagreRefusjonUtfall(it) }
        val foersteLoepenr = refusjonUtfallRepository.hentRefusjonUtfall(foerste.refusjonUtfallId)!!.loepenr

        val resultat =
            refusjonUtfallRepository.hentRefusjonUtfall(
                VedtakFilter(orgnr = foerste.orgnr.toString(), fraLoepenr = foersteLoepenr),
            )

        resultat.map { it.refusjonUtfall } shouldBe listOf(andre, tredje)
    }

    @Test
    fun `hentRefusjonUtfall med filter skal filtrere paa opprettet-dato med fom og tom`() {
        val refusjonUtfall = refusjonUtfallMock()
        refusjonUtfallRepository.lagreRefusjonUtfall(refusjonUtfall)
        val orgnr = refusjonUtfall.orgnr.toString()
        val idag = LocalDate.now()

        refusjonUtfallRepository.hentRefusjonUtfall(VedtakFilter(orgnr = orgnr, fom = idag, tom = idag)) shouldHaveSize 1
        refusjonUtfallRepository.hentRefusjonUtfall(VedtakFilter(orgnr = orgnr, fom = idag.plusDays(1))) shouldHaveSize 0
        refusjonUtfallRepository.hentRefusjonUtfall(VedtakFilter(orgnr = orgnr, tom = idag.minusDays(1))) shouldHaveSize 0
    }

    private fun hentRader(refusjonUtfallId: UUID) =
        transaction(db) {
            RefusjonUtfallEntitet
                .selectAll()
                .where { RefusjonUtfallEntitet.refusjonUtfallId eq refusjonUtfallId }
                .toList()
        }
}
