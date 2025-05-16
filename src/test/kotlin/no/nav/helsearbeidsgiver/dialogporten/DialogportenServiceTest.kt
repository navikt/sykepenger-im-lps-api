package no.nav.helsearbeidsgiver.dialogporten

import io.kotest.assertions.throwables.shouldThrowExactly
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DialogportenServiceTest {
    val mockDialogProducer = mockk<DialogProducer>()
    val mockUnleashFeatureToggles = mockk<UnleashFeatureToggles>()
    val dialogportenService = DialogportenService(mockDialogProducer, mockUnleashFeatureToggles)

    @Test
    fun `dialogporten service kaller dialogProducer`() {
        val dialogMelding = genererDialogMelding()

        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(dialogMelding.orgnr) } returns true

        dialogportenService.opprettNyDialogMedSykmelding(dialogMelding)

        verify(exactly = 1) {
            mockDialogProducer.send(dialogMelding)
        }
    }

    @Test
    fun `dialogporten service kaster feil dersom opprettelse av dialog går galt`() {
        val dialogMelding = genererDialogMelding()
        coEvery {
            mockDialogProducer.send(
                any(),
            )
        } throws SerializationException("Noe gikk galt")
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(dialogMelding.orgnr) } returns true

        shouldThrowExactly<SerializationException> {
            dialogportenService.opprettNyDialogMedSykmelding(
                dialogMelding,
            )
        }
    }

    @Test
    fun `dialogporten service kaller _ikke_ dialogProducer dersom feature toggle for dialogutsending er skrudd av`() {
        val dialogMelding = genererDialogMelding()
        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(dialogMelding.orgnr) } returns false

        dialogportenService.opprettNyDialogMedSykmelding(dialogMelding)

        verify(exactly = 0) {
            mockDialogProducer.send(any())
        }
    }

    private fun genererDialogMelding(): DialogSykmelding =
        DialogSykmelding(
            sykmeldingId = UUID.randomUUID(),
            orgnr = Orgnr.genererGyldig(),
            foedselsdato = LocalDate.now(),
            fulltNavn = "Dialogus Sykmeldingus",
            sykmeldingsperioder = listOf(Periode(fom = 1.januar, tom = 12.januar)),
        )
}
