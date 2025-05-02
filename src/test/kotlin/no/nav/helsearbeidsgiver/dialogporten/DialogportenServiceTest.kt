package no.nav.helsearbeidsgiver.dialogporten

import io.kotest.assertions.throwables.shouldThrowExactly
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DialogportenServiceTest {
    val mockDialogportenClient = mockk<DialogportenClient>()
    val mockDialogProducer = mockk<DialogProducer>()
    val dialogportenService = DialogportenService(mockDialogportenClient, mockDialogProducer)

    @Test
    fun `dialogporten service kaller dialogProducer`() {
        val dialogMelding =
            DialogSykmelding(
                sykmeldingId = UUID.randomUUID(),
                orgnr = Orgnr.genererGyldig(),
                foedselsdato = LocalDate.now(),
                fulltNavn = "John Smith",
                sykmeldingsperioder = listOf(Periode(fom = 1.januar, tom = 12.januar)),
            )

        coEvery { mockDialogProducer.send(any()) } just Runs

        dialogportenService.opprettNyDialogMedSykmelding(dialogMelding)

        verify(exactly = 1) {
            mockDialogProducer.send(dialogMelding)
        }
    }

    @Test
    fun `dialogporten service kaster feil dersom opprettelse av dialog går galt`() {
        val dialogMelding =
            DialogSykmelding(
                sykmeldingId = UUID.randomUUID(),
                orgnr = Orgnr.genererGyldig(),
                foedselsdato = LocalDate.now(),
                fulltNavn = "John Smith",
                sykmeldingsperioder = listOf(Periode(fom = 1.januar, tom = 12.januar)),
            )

        coEvery {
            mockDialogProducer.send(
                any(),
            )
        } throws SerializationException("Noe gikk galt")

        shouldThrowExactly<SerializationException> {
            dialogportenService.opprettNyDialogMedSykmelding(
                dialogMelding,
            )
        }
    }
}
