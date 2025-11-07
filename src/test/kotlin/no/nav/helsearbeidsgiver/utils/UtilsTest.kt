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

    @Test
    fun naisLeaderConfig() {
        // Dersom vi kjører NaisLeaderConfig uten nais enablet, vil default verdi for isElectedLeader() være false..
        // Tenker dette er tryggest i en feil-situasjon, da vil ikke to poder spise bakgrunnsjobber
        NaisLeaderConfig.isElectedLeader() shouldBe false
    }
}
