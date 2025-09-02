package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import org.junit.jupiter.api.Test
import java.util.UUID

class UnderkjentInntektsmeldingServiceTest {
    private val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    private val underkjentInntektsmeldingService = UnderkjentInntektsmeldingService(inntektsmeldingRepository)

    @Test
    fun `oppdaterInnteksmeldingTilFeilet skal kalle inntektsmeldingRepository`() {
        val underkjentInntektsmeldingMock =
            UnderkjentInntektsmelding(
                inntektsmeldingId = UUID.randomUUID(),
                feilkode = Valideringsfeil.Feilkode.INNTEKT_A_ORDNINGEN_AVVIK_MANGLER_AARSAK,
            )

        every {
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(
                any(),
            )
        } returns 1

        underkjentInntektsmeldingService.oppdaterInnteksmeldingTilFeilet(underkjentInntektsmeldingMock)

        verify(exactly = 1) {
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(
                underkjentInntektsmeldingMock,
            )
        }
    }
}
