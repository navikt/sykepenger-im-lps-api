package no.nav.helsearbeidsgiver.utils

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
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
        inntektsmelding.validerMotForespoersel(forespoersel, forespoersel.orgnr) shouldBe null
    }

    @Test
    fun validerStatus() {
        val inaktivForespoersel = forespoersel.copy(status = Status.FORKASTET)
        inntektsmelding.validerMotForespoersel(inaktivForespoersel, forespoersel.orgnr) shouldBe Feilmelding.FORESPOERSEL_FORKASTET
        val besvartForespoersel = forespoersel.copy(status = Status.BESVART)
        inntektsmelding.validerMotForespoersel(besvartForespoersel, besvartForespoersel.orgnr) shouldBe Feilmelding.UGYLDIG_AARSAK
    }

    @Test
    fun validerAtNavReferanseIdMatcher() {
        val forespoerselMedFeilReferanse = forespoersel.copy(navReferanseId = UUID.randomUUID())
        inntektsmelding.validerMotForespoersel(forespoerselMedFeilReferanse, forespoerselMedFeilReferanse.orgnr) shouldBe
            Feilmelding.UGYLDIG_REFERANSE
    }

    @Test
    fun validerAtSykmeldtMatcher() {
        val forespoerselMedForskjelligSykmeldt = forespoersel.copy(fnr = Fnr.genererGyldig().verdi)
        inntektsmelding.validerMotForespoersel(forespoerselMedForskjelligSykmeldt, forespoerselMedForskjelligSykmeldt.orgnr) shouldBe
            Feilmelding.FEIL_FNR
    }

    @Test
    fun validerAtOrgnrMatcher() {
        inntektsmelding.validerMotForespoersel(forespoersel, Orgnr.genererGyldig().verdi) shouldBe Feilmelding.FEIL_ORGNR
    }
}
