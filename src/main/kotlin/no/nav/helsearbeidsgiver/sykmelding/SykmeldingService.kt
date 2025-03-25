package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    fun hentSykmelding(
        id: UUID,
        orgnr: String,
    ): SykmeldingResponse? {
        try {
            val response = sykmeldingRepository.hentSykmelding(id).takeIf { it?.orgnr == orgnr }
            if (response != null) {
                sikkerLogger().info("Hentet sykmelding $id for orgnr: $orgnr")
            } else {
                sikkerLogger().info("Fant ingen sykmeldinger $id for orgnr: $orgnr")
            }
            return response
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av sykmelding $id orgnr: $orgnr", e)
        }
    }
}
