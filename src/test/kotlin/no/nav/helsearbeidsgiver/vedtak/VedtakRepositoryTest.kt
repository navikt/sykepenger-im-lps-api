package no.nav.helsearbeidsgiver.vedtak

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.kafka.sis.VedtaksUtfall
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

@WithPostgresContainer
class VedtakRepositoryTest {
    private val db: Database by lazy {
        DatabaseConfig(
            System.getProperty("database.url"),
            System.getProperty("database.username"),
            System.getProperty("database.password"),
        ).init()
    }
    private val vedtakRepository: VedtakRepository by lazy { VedtakRepository(db) }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { VedtakEntitet.deleteAll() }
    }

    @Test
    fun `lagreVedtak skal lagre vedtaket med sentrale felter i egne kolonner og hele meldingen som jsonb`() {
        val vedtak = vedtakMock()
        val forventetVedtakId = UUID.randomUUID()

        vedtakRepository.lagreVedtak(
            vedtakId = forventetVedtakId,
            vedtaksperiodeId = vedtak.vedtaksperiodeId,
            fnr = vedtak.foedselsnummer,
            orgnr = vedtak.organisasjonsnummer,
            vedtak = vedtak,
        )

        val lagredeRader = hentVedtak(vedtak.vedtaksperiodeId)

        lagredeRader shouldHaveSize 1
        lagredeRader.single()[VedtakEntitet.vedtakId] shouldBe forventetVedtakId
        lagredeRader.single()[VedtakEntitet.fnr] shouldBe vedtak.foedselsnummer.toString()
        lagredeRader.single()[VedtakEntitet.orgnr] shouldBe vedtak.organisasjonsnummer.toString()
        lagredeRader.single()[VedtakEntitet.vedtak] shouldBe vedtak
    }

    @Test
    fun `lagreVedtak skal tillate flere vedtak for samme vedtaksperiodeId, feks ved reberegning`() {
        val vedtak = vedtakMock()
        val vedtaksperiodeId = vedtak.vedtaksperiodeId
        val reberegnetVedtak = vedtak.copy(sykepengegrunnlag = vedtak.sykepengegrunnlag + 1000.0)
        val forventetVedtakId = UUID.randomUUID()
        val forventetReberegnetVedtakId = UUID.randomUUID()

        vedtakRepository.lagreVedtak(
            vedtakId = forventetVedtakId,
            vedtaksperiodeId = vedtaksperiodeId,
            fnr = vedtak.foedselsnummer,
            orgnr = vedtak.organisasjonsnummer,
            vedtak = vedtak,
        )
        vedtakRepository.lagreVedtak(
            vedtakId = forventetReberegnetVedtakId,
            vedtaksperiodeId = vedtaksperiodeId,
            fnr = reberegnetVedtak.foedselsnummer,
            orgnr = reberegnetVedtak.organisasjonsnummer,
            vedtak = reberegnetVedtak,
        )

        val lagredeRader = hentVedtak(vedtaksperiodeId)

        lagredeRader shouldHaveSize 2
        lagredeRader.map { it[VedtakEntitet.vedtak] } shouldBe listOf(vedtak, reberegnetVedtak)
        lagredeRader.map { it[VedtakEntitet.vedtakId] } shouldBe listOf(forventetVedtakId, forventetReberegnetVedtakId)
        forventetVedtakId shouldNotBe forventetReberegnetVedtakId
    }

    @Test
    fun `hentVedtak skal hente vedtak med loepenr`() {
        val vedtak = vedtakMock()
        val vedtakId = UUID.randomUUID()
        vedtakRepository.lagreVedtak(
            vedtakId = vedtakId,
            vedtaksperiodeId = vedtak.vedtaksperiodeId,
            fnr = vedtak.foedselsnummer,
            orgnr = vedtak.organisasjonsnummer,
            vedtak = vedtak,
        )
        val forventetLoepenr =
            transaction(db) {
                VedtakEntitet
                    .selectAll()
                    .where { VedtakEntitet.vedtakId eq vedtakId }
                    .single()[VedtakEntitet.id]
            }

        val lagretVedtak = vedtakRepository.hentVedtak(vedtakId)

        lagretVedtak?.loepenr shouldBe forventetLoepenr
        lagretVedtak?.vedtakId shouldBe vedtakId
        lagretVedtak?.vedtak shouldBe vedtak
    }

    @Test
    fun `hentVedtak skal filtrere paa orgnr fnr opprettet loepenr og vedtaksutfall`() {
        val vedtak = vedtakMock()
        val orgnr = vedtak.organisasjonsnummer
        val fnr = vedtak.foedselsnummer
        val vedtakFoerFraLoepenr = vedtak.copy(vedtaksUtfallTilArbeidsgiver = VedtaksUtfall.INNVILGELSE)
        val forventetVedtak = vedtak.copy(vedtaksUtfallTilArbeidsgiver = VedtaksUtfall.INNVILGELSE)
        val vedtakMedFeilUtfall = vedtak.copy(vedtaksUtfallTilArbeidsgiver = VedtaksUtfall.AVSLAG)

        val vedtakFoerFraLoepenrId = lagreVedtak(vedtakFoerFraLoepenr, fnr, orgnr)
        val forventetVedtakId = lagreVedtak(forventetVedtak, fnr, orgnr)
        lagreVedtak(vedtakMedFeilUtfall, fnr, orgnr)
        lagreVedtak(forventetVedtak, Fnr.genererGyldig(), orgnr)
        lagreVedtak(forventetVedtak, fnr, Orgnr.genererGyldig())
        val loepenrGrense =
            transaction(db) {
                VedtakEntitet
                    .selectAll()
                    .where { VedtakEntitet.vedtakId eq vedtakFoerFraLoepenrId }
                    .single()[VedtakEntitet.id]
            }

        val resultat =
            vedtakRepository.hentVedtak(
                VedtakFilter(
                    orgnr = orgnr.toString(),
                    fnr = fnr.toString(),
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    fraLoepenr = loepenrGrense,
                    vedtaksUtfall = VedtaksUtfall.INNVILGELSE,
                ),
            )

        resultat shouldHaveSize 1
        resultat.single().vedtakId shouldBe forventetVedtakId
    }

    private fun lagreVedtak(
        vedtak: VedtakArbeidsgiverMelding,
        fnr: Fnr,
        orgnr: Orgnr,
    ): UUID =
        UUID.randomUUID().also {
            vedtakRepository.lagreVedtak(
                vedtakId = it,
                vedtaksperiodeId = vedtak.vedtaksperiodeId,
                fnr = fnr,
                orgnr = orgnr,
                vedtak = vedtak,
            )
        }

    private fun hentVedtak(vedtaksperiodeId: UUID) =
        transaction(db) {
            VedtakEntitet
                .selectAll()
                .where { VedtakEntitet.vedtaksperiodeId eq vedtaksperiodeId }
                .toList()
        }
}
