package no.nav.helsearbeidsgiver.utils

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import org.junit.jupiter.api.Test
import java.util.UUID

class InnsendingUtilsTest {
    val forespoersel = mockForespoersel()
    val inntektsmelding =
        mockInntektsmeldingRequest().copy(
            navReferanseId = forespoersel.navReferanseId,
            sykmeldtFnr = forespoersel.fnr,
        )

    @Test
    fun validerOKMotForespoersel() {
        inntektsmelding.validerMotForespoersel(forespoersel) shouldBe null
    }

    @Test
    fun validerStatus() {
        val inaktivForespoersel = forespoersel.copy(status = Status.FORKASTET)
        inntektsmelding.validerMotForespoersel(inaktivForespoersel) shouldBe Feilmelding.FORESPOERSEL_FORKASTET
        val besvartForespoersel = forespoersel.copy(status = Status.BESVART)
        inntektsmelding.validerMotForespoersel(besvartForespoersel) shouldBe Feilmelding.UGYLDIG_AARSAK
    }

    @Test
    fun validerAtNavReferanseIdMatcher() {
        val forespoerselMedFeilReferanse = forespoersel.copy(navReferanseId = UUID.randomUUID())
        inntektsmelding.validerMotForespoersel(forespoerselMedFeilReferanse) shouldBe
            Feilmelding.UGYLDIG_REFERANSE
    }

    @Test
    fun validerAtSykmeldtMatcher() {
        val forespoerselMedForskjelligSykmeldt = forespoersel.copy(fnr = Fnr.genererGyldig().verdi)
        inntektsmelding.validerMotForespoersel(forespoerselMedForskjelligSykmeldt) shouldBe
            Feilmelding.FEIL_FNR
    }
}
