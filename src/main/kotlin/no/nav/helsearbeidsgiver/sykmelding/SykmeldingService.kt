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
        runCatching {
            sikkerLogger().info("Henter sykmeldinger: id: $id, orgnr: $orgnr")

            sykmeldingRepository.hentSykmelding(id).takeIf { it?.orgnr == orgnr }
        }.onSuccess {
            if (it != null) {
                sikkerLogger().info("Hentet ${it.id} sykmelding for orgnr: $orgnr")
            } else {
                sikkerLogger().info("Fant ingen sykmeldinger for id: $id, orgnr: $orgnr")
            }
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av sykmelding id: $id, orgnr: $orgnr", it)
        }
        throw RuntimeException("Feil ved henting av sykmelding id: $id, orgnr: $orgnr")
    }
}
