package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.konverter
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.whitelistetForArbeidsgiver
import java.util.UUID

class SoeknadService(
    val soeknadRepository: SoeknadRepository,
) {
    fun hentSoeknader(orgnr: String): List<Sykepengesoeknad> =
        soeknadRepository.hentSoeknader(orgnr).map { it.whitelistetForArbeidsgiver().konverter() }

    fun hentSoeknad(
        soknadId: UUID,
        orgnr: String,
    ): Sykepengesoeknad? =
        soeknadRepository.hentSoeknad(soknadId)?.whitelistetForArbeidsgiver()?.konverter().takeIf {
            it?.arbeidsgiver?.orgnr ==
                orgnr
        }

    fun behandleSoeknad(soeknad: SykepengesoknadDTO) {
        if (!soeknad.skalLagresOgSendesTilArbeidsgiver()) {
            sikkerLogger().info("Søknad med id ${soeknad.id} ignoreres fordi den ikke skal lagres og sendes til arbeidsgiver.")
            return
        }

        if (soeknad.soeknadAlleredeLagret()) {
            sikkerLogger().info("Søknad med id ${soeknad.id} ignoreres fordi den allerede er lagret.")
            return
        }

        try {
            soeknadRepository.lagreSoknad(soeknad.validerPaakrevdeFelter())
            sikkerLogger().info("lagret søknad med id: ${soeknad.id}")
        } catch (e: IllegalArgumentException) {
            sikkerLogger().warn(
                "Ignorerer sykepengesøknad med id ${soeknad.id} fordi søknaden mangler et påkrevd felt.",
                e,
            )
        }
    }

    private fun SykepengesoknadDTO.skalLagresOgSendesTilArbeidsgiver(): Boolean =
        (
            erArbeidstakerSoeknad() ||
                erArbeidstakerMedGradertReiseTilskudd() ||
                erArbeidstakerMedBehandlingsdager()

        ) &&
            !erEttersendtTilNAV() &&
            this.status == SykepengesoknadDTO.SoknadsstatusDTO.SENDT &&
            this.sendtArbeidsgiver != null

    private fun SykepengesoknadDTO.validerPaakrevdeFelter(): LagreSoknad =
        LagreSoknad(
            soknadId = id,
            sykmeldingId = requireNotNull(sykmeldingId) { "SykmeldingId kan ikke være null" },
            fnr = fnr,
            orgnr = requireNotNull(arbeidsgiver?.orgnummer) { "Orgnummer kan ikke være null" },
            sykepengesoknad = this,
        )

    private fun SykepengesoknadDTO.erArbeidstakerSoeknad(): Boolean = this.type == SykepengesoknadDTO.SoknadstypeDTO.ARBEIDSTAKERE

    private fun SykepengesoknadDTO.erArbeidstakerMedGradertReiseTilskudd(): Boolean =
        this.arbeidssituasjon == SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD

    private fun SykepengesoknadDTO.erArbeidstakerMedBehandlingsdager(): Boolean =
        this.arbeidssituasjon == SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER

    private fun SykepengesoknadDTO.soeknadAlleredeLagret(): Boolean = soeknadRepository.hentSoeknad(id) != null

    private fun SykepengesoknadDTO.erEttersendtTilNAV() = sendtNav != null && sendtArbeidsgiver?.isBefore(sendtNav) ?: false
}
