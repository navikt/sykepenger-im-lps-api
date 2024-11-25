package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.UGYLDIG_FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.UGYLDIG_JSON
import java.sql.SQLException

class KafkaErrorHandlingTest :
    FunSpec({

        val db = Database.init()

        val forespoerselRepository = ForespoerselRepository(db)
        val mockMottakRepository = mockk<MottakRepository>()
        val forespoerselTolker = ForespoerselTolker(forespoerselRepository, mockMottakRepository)

        test(
            "feil mot db - skal kaste exception om man ikke kan lagre til mottak-tabell " +
                "- så vi ikke commiter kafka-offset når db er nede",
        ) {
            every { mockMottakRepository.opprett(any()) } throws SQLException()
            shouldThrow<SQLException> {
                forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
                forespoerselTolker.lesMelding(UGYLDIG_JSON)
            }
        }

        test("ugyldig forespørsel i forespørselMottatt-melding skal bare lagre til mottak og gå videre") {
            clearAllMocks()
            every { mockMottakRepository.opprett(any()) } returns 100
            val mockForespoerselRepository = mockk<ForespoerselRepository>()
            val mockConsumer = ForespoerselTolker(mockForespoerselRepository, mockMottakRepository)
            mockConsumer.lesMelding(UGYLDIG_FORESPOERSEL_MOTTATT)

            verify(exactly = 1) { mockMottakRepository.opprett(any()) }
            verify(exactly = 0) { mockForespoerselRepository.lagreForespoersel(any(), any()) }
        }
    })
