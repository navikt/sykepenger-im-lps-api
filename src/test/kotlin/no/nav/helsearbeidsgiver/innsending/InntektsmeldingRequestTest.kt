package no.nav.helsearbeidsgiver.innsending

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.helsearbeidsgiver.inntektsmelding.Avsender
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
                "1.2.3-alpha",
            ),
    )
    fun `valider gyldig fritekst`(fritekst: String) {
        mockInntektsmeldingRequest().copy(kontaktinformasjon = fritekst).valider() shouldBe emptySet()
        mockInntektsmeldingRequest().copy(avsender = Avsender(fritekst, fritekst)).valider() shouldBe emptySet()
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "",
                " ",
                "  ",
                "}{\"hei\":\"hopp\"}",
                "drop table students;---",
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
    fun `valider ugyldig fritekst skal feile`(fritekst: String) {
        mockInntektsmeldingRequest().copy(kontaktinformasjon = fritekst).valider().first() shouldStartWith
            "Ugyldig kontaktinformasjon"
        mockInntektsmeldingRequest().copy(avsender = Avsender(fritekst, "1")).valider().first() shouldStartWith "Ugyldig systemNavn"
        mockInntektsmeldingRequest().copy(avsender = Avsender("Simba", fritekst)).valider().first() shouldStartWith "Ugyldig systemVersjon"
    }
}
