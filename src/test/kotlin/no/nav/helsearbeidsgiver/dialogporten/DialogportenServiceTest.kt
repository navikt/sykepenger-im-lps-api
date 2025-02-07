package no.nav.helsearbeidsgiver.dialogporten

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
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
        coEvery { mockDialogportenClient.opprettDialog(any(), any()) } returns Result.success(forventetDialogId.toString())

        dialogportenService.opprettDialog(orgnr, forespoereslId).getOrThrow() shouldBe forventetDialogId.toString()
    }

    @Test
    fun `dialogporten service kaster ikke feil`() {
        val orgnr = "1234"
        val forespoereslId = UUID.randomUUID()
        coEvery { mockDialogportenClient.opprettDialog(any(), any()) } returns Result.failure(DialogportenClientException())
        shouldNotThrow<DialogportenClientException> { dialogportenService.opprettDialog(orgnr, forespoereslId) }
    }
}
