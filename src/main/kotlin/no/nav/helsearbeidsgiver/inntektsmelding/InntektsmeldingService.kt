package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database.getInntektsmeldingRepo
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class InntektsmeldingService {
    fun hentInntektsmeldingerByOrgNr(orgnr: String): List<Inntektsmelding> {
        runCatching {
            sikkerLogger().info("Henter inntektsmeldinger for orgnr: $orgnr")
            getInntektsmeldingRepo().hent(orgnr)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} inntektsmeldinger for orgnr: $orgnr")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av inntektsmeldinger for orgnr: $orgnr", it)
            return emptyList()
        }

        return emptyList()
    }
}
