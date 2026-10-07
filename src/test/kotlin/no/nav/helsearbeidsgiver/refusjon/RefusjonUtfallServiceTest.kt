package no.nav.helsearbeidsgiver.refusjon

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefusjonUtfallServiceTest {
    private val unleashFeatureToggles = mockk<UnleashFeatureToggles>()
    private val refusjonKlient = mockk<RefusjonKlient>(relaxed = true)
    private val refusjonUtfallRepository = mockk<RefusjonUtfallRepository>()
    private val refusjonUtfallService = RefusjonUtfallService(unleashFeatureToggles, refusjonKlient, refusjonUtfallRepository)

    @BeforeEach
    fun clean() {
        clearAllMocks()
    }

    @Test
    fun `sendVedtak sender vedtaket til refusjon når featuretoggle er på`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val vedtak = vedtakMock()

        refusjonUtfallService.sendVedtak(vedtak)

        verify(exactly = 1) { refusjonKlient.sendVedtak(vedtak) }
    }

    @Test
    fun `sendVedtak sender ikke vedtaket til refusjon når featuretoggle er av`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns false

        refusjonUtfallService.sendVedtak(vedtakMock())

        verify(exactly = 0) { refusjonKlient.sendVedtak(any()) }
    }

    @Test
    fun `hentRefusjonUtfall henter refusjonsutfall fra repository`() {
        val refusjonUtfall = refusjonUtfallMock()
        every { refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfall.refusjonUtfallId) } returns refusjonUtfall

        refusjonUtfallService.hentRefusjonUtfall(refusjonUtfall.refusjonUtfallId) shouldBe refusjonUtfall
    }
}
