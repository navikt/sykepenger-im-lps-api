package no.nav.helsearbeidsgiver.innsending

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingRequest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class InntektsmeldingRequestTest {
    @ParameterizedTest
    @ValueSource(
        strings =
            arrayOf(
                "Test testesen",
                "Blah",
                "HR-Avdelingen gruppe 4",
                "punktum.",
                "Organisasjonen Blåbærsyltetøy",
            ),
    )
    fun `valider gyldig kontaktinformasjon`(kontaktinformasjon: String) {
        mockInntektsmeldingRequest().copy(kontaktinformasjon = kontaktinformasjon).valider() shouldBe emptySet()
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "",
                " ",
                "  ",
                "Little Bobby Tables; drop table students;---",
                "Heia!",
                "$<&>",
                "alert('javascript er kult')",
                "Gjemmer noen kontrolltegn\\r\\n\\t{Cntrl}",
                "🍕",
                "Sorry André",
                "\\u{1234}",
                "A/S - neitakk",
            ],
    )
    fun `valider ugyldig kontaktinformasjon skal feile`(kontaktinformasjon: String) {
        mockInntektsmeldingRequest().copy(kontaktinformasjon = kontaktinformasjon).valider().first() shouldStartWith
            "Ugyldig kontaktinformasjon"
    }
}
