package no.nav.helsearbeidsgiver.dialogporten

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.junit.jupiter.api.Test
import java.util.UUID

class DialogportenServiceTest {
    val mockDialogportenClient = mockk<DialogportenClient>()
    val dialogportenService = DialogportenService(mockDialogportenClient)

    @Test
    fun `dialogporten klient videresender resultatet`() {
        val orgnr = "1234"
        val forespoereslId = UUID.randomUUID()
        val forventetDialogId = UUID.randomUUID()
        coEvery {
            mockDialogportenClient.opprettNyDialogMedSykmelding(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns forventetDialogId.toString()

        dialogportenService.opprettNyDialogMedSykmelding(
            orgnr,
            forespoereslId,
            sykmeldingMessage = sykmeldingMock(),
        ) shouldBe forventetDialogId.toString()
    }

    @Test
    fun `dialogporten service kaster feil dersom opprettelse av dialog går galt`() {
        val orgnr = "1234"
        val forespoereslId = UUID.randomUUID()
        coEvery {
            mockDialogportenClient.opprettNyDialogMedSykmelding(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } throws DialogportenClientException("Noe gikk galt")

        shouldThrowExactly<DialogportenClientException> {
            dialogportenService.opprettNyDialogMedSykmelding(
                orgnr,
                forespoereslId,
                sykmeldingMessage = sykmeldingMock(),
            )
        }
    }
}
