package no.nav.helsearbeidsgiver.vedtak

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@WithPostgresContainer
class VedtakRepositoryTest {
    private lateinit var db: Database
    private lateinit var vedtakRepository: VedtakRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        vedtakRepository = VedtakRepository(db)
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { VedtakEntitet.deleteAll() }
    }

    @Test
    fun `lagreVedtak skal lagre vedtaket med sentrale felter i egne kolonner og hele meldingen som jsonb`() {
        val vedtak = vedtakMock()

        vedtakRepository.lagreVedtak(
            LagreVedtak(
                vedtaksperiodeId = vedtak.vedtaksperiodeId,
                fnr = vedtak.foedselsnummer.toString(),
                orgnr = vedtak.organisasjonsnummer.toString(),
                vedtak = vedtak,
            ),
        )

        val lagredeRader = hentVedtak(vedtak.vedtaksperiodeId)

        lagredeRader shouldHaveSize 1
        lagredeRader.single()[VedtakEntitet.fnr] shouldBe vedtak.foedselsnummer.toString()
        lagredeRader.single()[VedtakEntitet.orgnr] shouldBe vedtak.organisasjonsnummer.toString()
        lagredeRader.single()[VedtakEntitet.vedtak] shouldBe vedtak
    }

    @Test
    fun `lagreVedtak skal tillate flere vedtak for samme vedtaksperiodeId, f eks ved reberegning`() {
        val vedtak = vedtakMock()
        val vedtaksperiodeId = vedtak.vedtaksperiodeId
        val reberegnetVedtak = vedtak.copy(sykepengegrunnlag = vedtak.sykepengegrunnlag + 1000.0)

        vedtakRepository.lagreVedtak(
            LagreVedtak(
                vedtaksperiodeId = vedtaksperiodeId,
                fnr = vedtak.foedselsnummer.toString(),
                orgnr = vedtak.organisasjonsnummer.toString(),
                vedtak = vedtak,
            ),
        )
        vedtakRepository.lagreVedtak(
            LagreVedtak(
                vedtaksperiodeId = vedtaksperiodeId,
                fnr = reberegnetVedtak.foedselsnummer.toString(),
                orgnr = reberegnetVedtak.organisasjonsnummer.toString(),
                vedtak = reberegnetVedtak,
            ),
        )

        val lagredeRader = hentVedtak(vedtaksperiodeId)

        lagredeRader shouldHaveSize 2
        lagredeRader.map { it[VedtakEntitet.vedtak] } shouldBe listOf(vedtak, reberegnetVedtak)
    }

    private fun hentVedtak(vedtaksperiodeId: UUID) =
        transaction(db) {
            VedtakEntitet
                .selectAll()
                .where { VedtakEntitet.vedtaksperiodeId eq vedtaksperiodeId }
                .toList()
        }
}
