package no.nav.helsearbeidsgiver.vedtak

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@WithPostgresContainer
class VedtakServiceTest {
    private val db: Database by lazy {
        DatabaseConfig(
            System.getProperty("database.url"),
            System.getProperty("database.username"),
            System.getProperty("database.password"),
        ).init()
    }
    private val vedtakRepository: VedtakRepository by lazy { VedtakRepository(db) }
    private val unleashFeatureToggles = mockk<UnleashFeatureToggles>()
    private val vedtakService: VedtakService by lazy { VedtakService(vedtakRepository, unleashFeatureToggles) }

    @BeforeEach
    fun clean() {
        transaction(db) { VedtakEntitet.deleteAll() }
        clearAllMocks()
    }

    @Test
    fun `lagreVedtak lagrer vedtaket når featuretoggle er på`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val vedtak = vedtakMock()

        vedtakService.lagreVedtak(vedtak)

        val lagretVedtak = transaction(db) { VedtakEntitet.selectAll().firstOrNull()?.getOrNull(VedtakEntitet.vedtak) }
        lagretVedtak shouldBe vedtak
    }

    @Test
    fun `lagreVedtak lagrer ikke vedtaket når featuretoggle er av`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns false

        vedtakService.lagreVedtak(vedtakMock())

        val lagretVedtak = transaction(db) { VedtakEntitet.selectAll().firstOrNull()?.getOrNull(VedtakEntitet.vedtak) }
        lagretVedtak shouldBe null
    }
}
