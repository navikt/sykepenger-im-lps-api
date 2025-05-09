package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SoknadService(
    val soknadRepository: SoknadRepository,
) {
    fun lagreSoknad(soknad: SykepengesoknadDTO) {
        try {
            soknadRepository.lagreSoknad(soknad.validerPaakrevdeFelter())
        } catch (e: IllegalArgumentException) {
            sikkerLogger().error(
                "Ignorerer sykepengesøknad med id ${soknad.id} fordi søknaden mangler et påkrevd felt.",
                e,
            )
        }
    }

    private fun SykepengesoknadDTO.validerPaakrevdeFelter(): LagreSoknad =
        LagreSoknad(
            soknadId = id,
            sykmeldingId = requireNotNull(sykmeldingId) { "SykmeldingId kan ikke være null" },
            fnr = fnr,
            orgnr = requireNotNull(arbeidsgiver?.orgnummer) { "Orgnummer kan ikke være null" },
            sykepengesoknad = this,
        )
}
