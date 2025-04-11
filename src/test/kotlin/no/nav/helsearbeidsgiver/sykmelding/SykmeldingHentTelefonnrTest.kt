package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.sykmelding.model.hentTelefonnr
import org.junit.jupiter.api.Test

class SykmeldingHentTelefonnrTest {
    @Test
    fun `returnerer hentTelefonnr det samme som gammel regex versjon`() {
        listOf(
            "12345678" to "12345678",
            " tel:12345678" to "12345678",
            "fax:12345678 " to "12345678",
            null to "",
        ).forEach { (input, expected) ->
            hentTelefonnr(input) shouldBe expected
        }
    }
}
