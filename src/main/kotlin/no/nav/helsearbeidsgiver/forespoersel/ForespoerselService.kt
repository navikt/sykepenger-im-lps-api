package no.nav.helsearbeidsgiver.forespoersel

import java.util.UUID

class ForespoerselService {
    fun hentForespoersler() : List<Forespoersel> {
        return listOf(
            Forespoersel(ForespoerselType.KOMPLETT, "orgnr", "fnr", UUID.randomUUID(), emptyList(), emptyList(), emptyMap(),
                ForespurtData(ForespurtData.Arbeidsgiverperiode(true), ForespurtData.Inntekt(true, ForslagInntekt.Grunnlag(null)), ForespurtData.Refusjon(true, ForslagRefusjon(emptyList(), null))), false),
            Forespoersel(ForespoerselType.BEGRENSET, "orgnr", "fnr", UUID.randomUUID(), emptyList(), emptyList(), emptyMap(),
                ForespurtData(ForespurtData.Arbeidsgiverperiode(true), ForespurtData.Inntekt(true, ForslagInntekt.Grunnlag(null)), ForespurtData.Refusjon(true, ForslagRefusjon(emptyList(), null))), false),
            Forespoersel(ForespoerselType.POTENSIELL, "orgnr", "fnr", UUID.randomUUID(), emptyList(), emptyList(), emptyMap(),
                ForespurtData(ForespurtData.Arbeidsgiverperiode(true), ForespurtData.Inntekt(true, ForslagInntekt.Grunnlag(null)), ForespurtData.Refusjon(true, ForslagRefusjon(emptyList(), null))), false),
            Forespoersel(ForespoerselType.KOMPLETT, "orgnr", "fnr", UUID.randomUUID(), emptyList(), emptyList(), emptyMap(),
                ForespurtData(ForespurtData.Arbeidsgiverperiode(true), ForespurtData.Inntekt(true, ForslagInntekt.Grunnlag(null)), ForespurtData.Refusjon(true, ForslagRefusjon(emptyList(), null))), true),
        )
    }
}


