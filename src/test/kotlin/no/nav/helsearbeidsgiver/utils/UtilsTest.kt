package no.nav.helsearbeidsgiver.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtilsTest {
    @Test
    fun tilTidspunktStartOfDay() {
        val tid = LocalDate.now().tilTidspunktStartOfDay()
        tid.hour shouldBe 0
        tid.minute shouldBe 0
        tid.nano shouldBe 0
    }
}
