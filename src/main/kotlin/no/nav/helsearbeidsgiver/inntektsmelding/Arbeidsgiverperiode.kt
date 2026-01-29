package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RedusertLoennIAgp
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode as ArbeidsgiverperiodeMedEgenmeldinger

// TODO: Fjernes når vi bumper til hag-domene 0.9
@Serializable
data class Arbeidsgiverperiode(
    val perioder: List<Periode>,
    val redusertLoennIAgp: RedusertLoennIAgp?,
) {
    fun tilArbeidsgiverperiodeMedEgenmeldinger(): ArbeidsgiverperiodeMedEgenmeldinger =
        ArbeidsgiverperiodeMedEgenmeldinger(perioder, emptyList(), redusertLoennIAgp)
}
