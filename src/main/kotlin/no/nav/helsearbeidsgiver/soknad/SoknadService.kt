package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SoknadService(
    val soknadRepository: SoknadRepository,
) {
    fun behandleSoknad(soknad: SykepengesoknadDTO) {
        if (!soknad.skalLagresOgSendesTilArbeidsgiver()) {
            sikkerLogger().info("Søknad med id ${soknad.id} ignoreres fordi den ikke skal lagres og sendes til arbeidsgiver.")
            return
        }

        if (soknad.soknadAlleredeLagret()) {
            sikkerLogger().info("Søknad med id ${soknad.id} ignoreres fordi den allerede er lagret.")
            return
        }

        try {
            soknadRepository.lagreSoknad(soknad.validerPaakrevdeFelter())
        } catch (e: IllegalArgumentException) {
            sikkerLogger().warn(
                "Ignorerer sykepengesøknad med id ${soknad.id} fordi søknaden mangler et påkrevd felt.",
                e,
            )
        }
    }

    private fun SykepengesoknadDTO.skalLagresOgSendesTilArbeidsgiver(): Boolean =
        (
            erArbeidstakerSoknad() ||
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

    private fun SykepengesoknadDTO.erArbeidstakerSoknad(): Boolean = this.type == SykepengesoknadDTO.SoknadstypeDTO.ARBEIDSTAKERE

    private fun SykepengesoknadDTO.erArbeidstakerMedGradertReiseTilskudd(): Boolean =
        this.arbeidssituasjon == SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD

    private fun SykepengesoknadDTO.erArbeidstakerMedBehandlingsdager(): Boolean =
        this.arbeidssituasjon == SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER

    private fun SykepengesoknadDTO.soknadAlleredeLagret(): Boolean = soknadRepository.hentSoknad(id) != null

    private fun SykepengesoknadDTO.erEttersendtTilNAV() = sendtNav != null && sendtArbeidsgiver?.isBefore(sendtNav) ?: false
}
