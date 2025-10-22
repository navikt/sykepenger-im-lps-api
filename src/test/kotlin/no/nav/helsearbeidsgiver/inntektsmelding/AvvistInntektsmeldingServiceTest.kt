package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import org.junit.jupiter.api.Test
import java.util.UUID

class AvvistInntektsmeldingServiceTest {
    private val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    private val dialogportenService = mockk<DialogportenService>()
    private val avvistInntektsmeldingService =
        AvvistInntektsmeldingService(inntektsmeldingRepository = inntektsmeldingRepository, dialogportenService = dialogportenService)

    @Test
    fun `oppdaterInnteksmeldingTilFeilet skal kalle inntektsmeldingRepository`() {
        val avvistInntektsmeldingMock =
            AvvistInntektsmelding(
                inntektsmeldingId = UUID.randomUUID(),
                feilkode = Valideringsfeil.Feilkode.INNTEKT_AVVIKER_FRA_A_ORDNINGEN,
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
