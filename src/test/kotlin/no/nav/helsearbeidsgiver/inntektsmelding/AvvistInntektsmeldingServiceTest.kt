package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.Test
import java.util.UUID

class AvvistInntektsmeldingServiceTest {
    private val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    private val dokumentKoblingService = mockk<DokumentkoblingService>()
    private val avvistInntektsmeldingService =
        AvvistInntektsmeldingService(
            inntektsmeldingRepository = inntektsmeldingRepository,
            dokumentkoblingService = dokumentKoblingService,
        )

    @Test
    fun `oppdaterInnteksmeldingTilFeilet skal kalle inntektsmeldingRepository`() {
        val inntektsmeldingId = UUID.randomUUID()
        val avvistInntektsmeldingMock =
            AvvistInntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                forespoerselId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID(),
                orgnr = Orgnr.genererGyldig(),
                feil = Valideringsfeil(Valideringsfeil.Feilkode.INNTEKT_AVVIKER_FRA_A_ORDNINGEN, null),
            )

        every {
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(
                any(),
            )
        } returns 1
        every {
            inntektsmeldingRepository.hentMedInnsendingId(avvistInntektsmeldingMock.inntektsmeldingId)
        } returns mockInntektsmeldingResponse()

        every {
            dokumentKoblingService.produserInntektsmeldingAvvistKobling(
                avvistInntektsmeldingMock,
            )
        } just Runs
        avvistInntektsmeldingService.oppdaterInnteksmeldingTilFeilet(avvistInntektsmeldingMock)

        verify(exactly = 1) {
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(
                avvistInntektsmeldingMock,
            )
        }
    }

    @Test
    fun `oppdaterInnteksmeldingTilFeilet skal ikke overskrive en allerede godkjent IM dersom den får feil pga duplikat`() {
        // Status GODKJENT og FEILET er endelige statuser.
        // Hvis en IM er godkjent: teoretisk kunne API ha sendt samme IM på nytt i rare feilsituasjoner og fått duplikatfeil, eller Simba kunne
        // re-sendt samme melding ved en feil. Vi vil bare avvise de som har status MOTTATT, ikke de som allerede er GODKJENT eller FEILET
        val alleredeGodkjent =
            AvvistInntektsmelding(
                inntektsmeldingId = UUID.randomUUID(),
                forespoerselId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID(),
                orgnr = Orgnr.genererGyldig(),
                feil = Valideringsfeil(Valideringsfeil.Feilkode.DUPLIKAT, "Duplikat"),
            )
        val alleredeFeilet =
            AvvistInntektsmelding(
                inntektsmeldingId = UUID.randomUUID(),
                forespoerselId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID(),
                orgnr = Orgnr.genererGyldig(),
                feil = Valideringsfeil(Valideringsfeil.Feilkode.DUPLIKAT, "Duplikat"),
            )

        every {
            inntektsmeldingRepository.hentMedInnsendingId(alleredeGodkjent.inntektsmeldingId)
        } returns mockInntektsmeldingResponse().copy(status = InnsendingStatus.GODKJENT)
        every {
            inntektsmeldingRepository.hentMedInnsendingId(alleredeFeilet.inntektsmeldingId)
        } returns mockInntektsmeldingResponse().copy(status = InnsendingStatus.FEILET)

        avvistInntektsmeldingService.oppdaterInnteksmeldingTilFeilet(alleredeGodkjent)
        avvistInntektsmeldingService.oppdaterInnteksmeldingTilFeilet(alleredeFeilet)

        verify(exactly = 0) {
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(alleredeGodkjent)
            dokumentKoblingService.produserInntektsmeldingAvvistKobling(alleredeGodkjent)
        }
    }
}
