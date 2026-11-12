package no.nav.helsearbeidsgiver.refusjon

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefusjonUtfallServiceTest {
    private val refusjonKlient = mockk<RefusjonKlient>(relaxed = true)
    private val refusjonUtfallRepository = mockk<RefusjonUtfallRepository>()
    private val refusjonUtfallService = RefusjonUtfallService(refusjonKlient, refusjonUtfallRepository)

    @BeforeEach
    fun clean() {
        clearAllMocks()
    }
// TODO: Slett..
//    @Test
//    fun `hentRefusjonUtfall henter refusjonsutfall fra repository`() {
//        val refusjonUtfall = refusjonUtfallResponseMock()
//        every { refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfall.refusjonUtfallId) } returns refusjonUtfall
//
//        refusjonUtfallService.hentRefusjonUtfall(refusjonUtfall.refusjonUtfallId) shouldBe refusjonUtfall
//    }
}
