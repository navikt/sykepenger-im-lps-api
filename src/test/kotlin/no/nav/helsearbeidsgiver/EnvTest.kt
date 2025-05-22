package no.nav.helsearbeidsgiver

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EnvTest {
    @Test
    fun `getPropertyOrNull gir null hvis property ikke er satt`() {
        val ikkeSatt = "ikkeSatt"
        Env.getPropertyOrNull(ikkeSatt) shouldBe null
    }

    @Test
    fun `getPropertyOrNull gir verdi hvis property er satt`() {
        Env.getPropertyOrNull("application.env") shouldBe "local"
    }

    @Test
    fun `getPropertyAsList gir liste hvis property er satt`() {
        val scope = "maskinporten.eksponert_scopes"
        Env.getPropertyAsList(scope) shouldBe listOf("nav:helse/im.read", "nav:helseytelser/sykepenger")
    }
}
