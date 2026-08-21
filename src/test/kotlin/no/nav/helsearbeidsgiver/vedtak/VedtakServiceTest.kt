package no.nav.helsearbeidsgiver.vedtak

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VedtakServiceTest {
    private val vedtakRepository = mockk<VedtakRepository>(relaxed = true)
    private val unleashFeatureToggles = mockk<UnleashFeatureToggles>()
    private val vedtakService = VedtakService(vedtakRepository, unleashFeatureToggles)

    @BeforeEach
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `lagreVedtak lagrer vedtaket når featuretoggle er på`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true

        vedtakService.lagreVedtak(vedtakMock())

        verify(exactly = 1) { vedtakRepository.lagreVedtak(any()) }
    }

    @Test
    fun `lagreVedtak lagrer ikke vedtaket når featuretoggle er av`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns false

        vedtakService.lagreVedtak(vedtakMock())

        verify(exactly = 0) { vedtakRepository.lagreVedtak(any()) }
    }
}
