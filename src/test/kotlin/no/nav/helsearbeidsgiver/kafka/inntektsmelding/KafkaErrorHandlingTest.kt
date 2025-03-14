package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.UGYLDIG_FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.UGYLDIG_JSON
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException

class KafkaErrorHandlingTest {
    val db = DbConfig.init()

    val forespoerselRepository = ForespoerselRepository(db)
    val mockMottakRepository = mockk<MottakRepository>()
    val mockDialogportenService = mockk<IDialogportenService>()
    val forespoerselTolker = ForespoerselTolker(forespoerselRepository, mockMottakRepository, mockDialogportenService)

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    @Test
    fun `feil mot db - skal kaste exception om man ikke kan lagre til mottak-tabell - så vi ikke commiter kafka-offset når db er nede`() {
        every { mockMottakRepository.opprett(any()) } throws SQLException()
        shouldThrow<SQLException> {
            forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
            forespoerselTolker.lesMelding(UGYLDIG_JSON)
        }
    }

    @Test
    fun `ugyldig forespørsel i forespørselMottatt-melding skal bare lagre til mottak og gå videre`() {
        every { mockMottakRepository.opprett(any()) } returns 100
        val mockForespoerselRepository = mockk<ForespoerselRepository>()
        val mockConsumer = ForespoerselTolker(mockForespoerselRepository, mockMottakRepository, mockDialogportenService)
        mockConsumer.lesMelding(UGYLDIG_FORESPOERSEL_MOTTATT)

        verify(exactly = 1) { mockMottakRepository.opprett(any()) }
        verify(exactly = 0) { mockForespoerselRepository.lagreForespoersel(any(), any()) }
    }
}
