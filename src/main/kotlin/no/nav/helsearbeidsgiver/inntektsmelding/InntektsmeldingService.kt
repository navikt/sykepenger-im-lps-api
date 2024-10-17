package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

class InntektsmeldingService {
    fun hentInntektsmeldingerByOrgNr(orgnr: Orgnr): List<Inntektsmelding> = emptyList()
}
