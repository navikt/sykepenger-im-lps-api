package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.konverter
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.whitelistetForArbeidsgiver
import java.util.UUID

class SoknadService(
    private val soknadRepository: SoknadRepository,
) {
    fun hentSoknader(orgnr: String): List<Sykepengesoknad> =
        soknadRepository.hentSoknader(orgnr).map { it.whitelistetForArbeidsgiver().konverter() }

    fun hentSoknad(
        soknadId: UUID,
        orgnr: String,
    ): Sykepengesoknad? =
        soknadRepository.hentSoknad(soknadId)?.whitelistetForArbeidsgiver()?.konverter().takeIf {
            it?.arbeidsgiver?.orgnummer ==
                orgnr
        }

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
