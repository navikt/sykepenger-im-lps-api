package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.UGYLDIG_FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.UGYLDIG_JSON
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException

@WithPostgresContainer
class KafkaErrorHandlingTest {
    private lateinit var db: Database
    private lateinit var forespoerselRepository: ForespoerselRepository

    val mockMottakRepository = mockk<MottakRepository>()

    private lateinit var forespoerselTolker: ForespoerselTolker

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        forespoerselRepository = ForespoerselRepository(db)
        forespoerselTolker = ForespoerselTolker(forespoerselRepository, mockMottakRepository)
    }

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
        val mockConsumer = ForespoerselTolker(mockForespoerselRepository, mockMottakRepository)
        mockConsumer.lesMelding(UGYLDIG_FORESPOERSEL_MOTTATT)

        verify(exactly = 1) { mockMottakRepository.opprett(any()) }
        verify(exactly = 0) { mockForespoerselRepository.lagreForespoersel(any(), any()) }
    }
}
