package no.nav.helsearbeidsgiver.utils

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.til
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.utils.test.date.februar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.collections.listOf

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
    fun `godtar inntektsmelding dersom egenmeldinger i AGP er gyldige`() {
        val forespoerselIFebruar =
            forespoersel.copy(
                sykmeldingsperioder =
                    listOf(
                        1.februar til 12.februar,
                        19.februar til 28.februar,
                    ),
                egenmeldingsperioder = emptyList(), // fjerner for å vektlegge at det ikke er disse egenmeldingene vi validerer
            )
        val imMedGyldigeEgenmenldinger =
            inntektsmelding.copy(
                agp =
                    Arbeidsgiverperiode(
                        perioder =
                            listOf(
                                1.februar til 12.februar,
                                14.februar til 15.februar, // egenmelding er gyldig pga. gjenopptatt arbeid 13.
                                19.februar til 20.februar,
                            ),
                        redusertLoennIAgp = null,
                    ),
            )

        imMedGyldigeEgenmenldinger.validerMotForespoersel(forespoerselIFebruar).shouldBeNull()
    }

    @Test
    fun `avviser inntektsmelding dersom egenmeldinger i AGP er ugyldige`() {
        val forespoerselIFebruar =
            forespoersel.copy(
                sykmeldingsperioder =
                    listOf(
                        1.februar til 12.februar,
                        19.februar til 28.februar,
                    ),
                egenmeldingsperioder = emptyList(), // fjerner for å vektlegge at det ikke er disse egenmeldingene vi validerer
            )
        val imMedUgyldigeEgenmenldinger =
            inntektsmelding.copy(
                agp =
                    Arbeidsgiverperiode(
                        perioder =
                            listOf(
                                1.februar til 14.februar, // egenmelding 13.-14. er ikke gyldig ettersom arbeid ikke er gjenopptatt før 13.
                                19.februar til 20.februar,
                            ),
                        redusertLoennIAgp = null,
                    ),
            )

        imMedUgyldigeEgenmenldinger.validerMotForespoersel(forespoerselIFebruar) shouldBe
            "Ugyldig arbeidsgiverperiode. Egenmelding kan ikke benyttes dagen etter en sykmeldingsperiode."
    }

    @Test
    fun `validerer OK for manglende, ikke-forespurt AGP`() {
        val forespoerselUtenPaakrevdAgp = forespoersel.copy(arbeidsgiverperiodePaakrevd = false)
        val inntektsmeldingUtenAgp = inntektsmelding.copy(agp = null)

        inntektsmeldingUtenAgp.validerMotForespoersel(forespoerselUtenPaakrevdAgp).shouldBeNull()
    }

    @Test
    fun `validerer OK for gyldig, ikke-forespurt AGP`() {
        val forespoerselUtenPaakrevdAgp =
            forespoersel.copy(
                arbeidsgiverperiodePaakrevd = false,
                sykmeldingsperioder = listOf(1.februar til 28.februar),
                egenmeldingsperioder = emptyList(),
            )
        val inntektsmeldingMedGyldigAgp =
            inntektsmelding.copy(
                agp =
                    Arbeidsgiverperiode(
                        perioder = listOf(2.februar til 17.februar),
                        redusertLoennIAgp = null,
                    ),
            )

        inntektsmeldingMedGyldigAgp.validerMotForespoersel(forespoerselUtenPaakrevdAgp).shouldBeNull()
    }

    @Test
    fun `gir feilmelding ved ugyldig, ikke-forespurt AGP`() {
        val forespoerselUtenPaakrevdAgp =
            forespoersel.copy(
                arbeidsgiverperiodePaakrevd = false,
                sykmeldingsperioder = listOf(1.februar til 28.februar),
                egenmeldingsperioder = emptyList(),
            )
        val inntektsmeldingMedUgyldigAgp =
            inntektsmelding.copy(
                agp =
                    Arbeidsgiverperiode(
                        perioder = listOf(1.februar til 16.februar),
                        redusertLoennIAgp = null,
                    ),
            )

        inntektsmeldingMedUgyldigAgp.validerMotForespoersel(forespoerselUtenPaakrevdAgp) shouldBe
            "Ugyldig arbeidsgiverperiode. Arbeidsgiverperioden må indikere at sykmeldt arbeidet i starten av sykefraværet."
    }
}
