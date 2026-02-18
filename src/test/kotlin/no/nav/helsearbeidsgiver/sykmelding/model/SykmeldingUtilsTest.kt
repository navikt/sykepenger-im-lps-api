package no.nav.helsearbeidsgiver.sykmelding.model

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.tilPerioder
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.helsearbeidsgiver.utils.kapitaliserNavn

class SykmeldingUtilsTest {
    @Test
    fun `List av LocalDate tilPerioder konverterer riktig`() {
        mockLocalDates(1, 2, 4).tilPerioder() shouldBe
            mockPerioder(1 to 2, 4 to 4)
    }

    @Test
    fun `List av LocalDate tilPerioder ignorerer duplikater`() {
        mockLocalDates(1, 1, 1, 5, 6, 6, 7).tilPerioder() shouldBe
            mockPerioder(1 to 1, 5 to 7)
    }

    @Test
    fun `List av LocalDate tilPerioder ignorer rekkefølge av datoer`() {
        mockLocalDates(4, 3, 1).tilPerioder() shouldBe
            mockPerioder(1 to 1, 3 to 4)
    }

    @Test
    fun `List av LocalDate tilPerioder uten dager returnerer tom set`() {
        emptySet<LocalDate>().tilPerioder() shouldBe emptySet()
    }

    @Test
    fun `kapitaliserNavn kapitaliserer som forventet med ulike navn`() {
        "OLA NORDMANN".kapitaliserNavn() shouldBe "Ola Nordmann"
        "JAN-ERIK OLA".kapitaliserNavn() shouldBe "Jan-Erik Ola"
        // aksepterer at disse edge case "McDonald" og "Vincent van Gogh" ikke kapitaliseres riktig
        "MCDONALD".kapitaliserNavn() shouldBe "Mcdonald"
        "VINCENT VAN GOGH".kapitaliserNavn() shouldBe "Vincent Van Gogh"
    }
}

fun mockLocalDates(vararg dag: Int): Set<LocalDate> = dag.asList().map { it.tilLocalDate() }.toSet()

fun mockPerioder(vararg pair: Pair<Int, Int>): Set<Periode> = pair.asList().map { it.tilMockPeriode() }.toSet()

fun Pair<Int, Int>.tilMockPeriode(): Periode = Periode(fom = first.tilLocalDate(), tom = second.tilLocalDate())

fun Int.tilLocalDate(): LocalDate = LocalDate.of(2023, 5, this)
