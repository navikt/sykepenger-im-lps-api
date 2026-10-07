package no.nav.helsearbeidsgiver.kafka.refusjon

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfall
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfallRepository
import no.nav.helsearbeidsgiver.refusjon.Utfall
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RefusjonUtfallTolkerTest {
    private val refusjonUtfallRepository = mockk<RefusjonUtfallRepository>()
    private val refusjonUtfallTolker = RefusjonUtfallTolker(refusjonUtfallRepository)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `lesMelding skal lagre refusjonsutfall fra melding produsert av hag-refusjon`() {
        val lagretRefusjonUtfall = slot<RefusjonUtfall>()
        every { refusjonUtfallRepository.lagreRefusjonUtfall(capture(lagretRefusjonUtfall)) } just Runs

        refusjonUtfallTolker.lesMelding(REFUSJON_UTFALL_MELDING)

        lagretRefusjonUtfall.captured shouldBe
            RefusjonUtfall(
                refusjonUtfallId = UUID.fromString("5f1e7a3c-0b2d-4c8e-9a6f-1d2e3f4a5b6c"),
                vedtaksperiodeId = UUID.fromString("c62594af-f0b8-4fd1-88f2-07e1b15dd906"),
                fnr = Fnr("10107400090"),
                orgnr = Orgnr("896929119"),
                fom = LocalDate.of(2026, 7, 28),
                tom = LocalDate.of(2026, 8, 3),
                sykepengegrunnlag = 154999.92,
                utfallTilArbeidsgiver = Utfall.INNVILGELSE,
                fattetTidspunkt = LocalDateTime.parse("2026-08-05T13:03:25.166498222"),
            )
    }

    @Test
    fun `lesMelding skal kaste feil og ikke lagre naar meldingen har feil format`() {
        assertThrows<SerializationException> {
            refusjonUtfallTolker.lesMelding("""{"refusjonUtfallId": "ikke-en-uuid"}""")
        }

        verify(exactly = 0) { refusjonUtfallRepository.lagreRefusjonUtfall(any()) }
    }

    @Test
    fun `lesMelding skal kaste feil videre naar lagring feiler`() {
        every { refusjonUtfallRepository.lagreRefusjonUtfall(any()) } throws RuntimeException("database nede")

        assertThrows<RuntimeException> { refusjonUtfallTolker.lesMelding(REFUSJON_UTFALL_MELDING) }
    }
}

private val REFUSJON_UTFALL_MELDING =
    """
    {
      "refusjonUtfallId": "5f1e7a3c-0b2d-4c8e-9a6f-1d2e3f4a5b6c",
      "vedtaksperiodeId": "c62594af-f0b8-4fd1-88f2-07e1b15dd906",
      "fnr": "10107400090",
      "orgnr": "896929119",
      "fom": "2026-07-28",
      "tom": "2026-08-03",
      "sykepengegrunnlag": 154999.92,
      "utfallTilArbeidsgiver": "INNVILGELSE",
      "fattetTidspunkt": "2026-08-05T13:03:25.166498222"
    }
    """.trimIndent()
