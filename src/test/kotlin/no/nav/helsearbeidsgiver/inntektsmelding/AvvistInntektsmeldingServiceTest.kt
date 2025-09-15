package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import org.junit.jupiter.api.Test
import java.util.UUID

class AvvistInntektsmeldingServiceTest {
    private val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    private val avvistInntektsmeldingService = AvvistInntektsmeldingService(inntektsmeldingRepository)

    @Test
    fun `oppdaterInnteksmeldingTilFeilet skal kalle inntektsmeldingRepository`() {
        val avvistInntektsmeldingMock =
            AvvistInntektsmelding(
                inntektsmeldingId = UUID.randomUUID(),
                feilkode = Valideringsfeil.Feilkode.INNTEKT_A_ORDNINGEN_AVVIK_MANGLER_AARSAK,
            )

        every {
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(
                any(),
            )
        } returns 1

        avvistInntektsmeldingService.oppdaterInnteksmeldingTilFeilet(avvistInntektsmeldingMock)

        verify(exactly = 1) {
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(
                avvistInntektsmeldingMock,
            )
        }
    }
}
