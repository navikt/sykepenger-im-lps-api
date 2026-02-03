package no.nav.helsearbeidsgiver.utils

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
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

    @Test
    fun `gir feilmelding ved manglende, forespurt AGP`() {
        val forespoerselUtenPaakrevdAgp = forespoersel.copy(arbeidsgiverperiodePaakrevd = true)
        val inntektsmeldingUtenAgp = inntektsmelding.copy(agp = null)

        inntektsmeldingUtenAgp.validerMotForespoersel(forespoerselUtenPaakrevdAgp) shouldBe Feilmelding.AGP_ER_PAAKREVD
    }

    @Test
    fun `validerer OK for manglende, ikke-forespurt AGP`() {
        val forespoerselUtenPaakrevdAgp = forespoersel.copy(arbeidsgiverperiodePaakrevd = false)
        val inntektsmeldingUtenAgp = inntektsmelding.copy(agp = null)

        inntektsmeldingUtenAgp.validerMotForespoersel(forespoerselUtenPaakrevdAgp).shouldBeNull()
    }

    @Test
    fun `validerer OK for gyldig, ikke-forespurt AGP`() {
        val forespoerselUtenPaakrevdAgp = forespoersel.copy(arbeidsgiverperiodePaakrevd = false)
        val inntektsmeldingMedGyldigAgp =
            inntektsmelding.copy(
                agp =
                    forespoerselUtenPaakrevdAgp.sykmeldingsperioder.minOf { it.fom }.let {
                        Arbeidsgiverperiode(
                            perioder =
                                listOf(
                                    Periode(
                                        fom = it.plusDays(1),
                                        tom = it.plusDays(17),
                                    ),
                                ),
                            redusertLoennIAgp = null,
                        )
                    },
            )

        inntektsmeldingMedGyldigAgp.validerMotForespoersel(forespoerselUtenPaakrevdAgp).shouldBeNull()
    }

    @Test
    fun `gir feilmelding ved ugyldig, ikke-forespurt AGP`() {
        val forespoerselUtenPaakrevdAgp = forespoersel.copy(arbeidsgiverperiodePaakrevd = false)
        val inntektsmeldingMedUgyldigAgp =
            inntektsmelding.copy(
                agp =
                    forespoerselUtenPaakrevdAgp.sykmeldingsperioder.minOf { it.fom }.let {
                        Arbeidsgiverperiode(
                            perioder =
                                listOf(
                                    Periode(
                                        fom = it,
                                        tom = it.plusDays(16),
                                    ),
                                ),
                            redusertLoennIAgp = null,
                        )
                    },
            )

        inntektsmeldingMedUgyldigAgp.validerMotForespoersel(forespoerselUtenPaakrevdAgp) shouldBe Feilmelding.AGP_IKKE_FORESPURT_ER_UGYLDIG
    }
}
