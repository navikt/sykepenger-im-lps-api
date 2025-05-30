package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.dialogporten.DialogSykepengesoknad
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.konverter
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.whitelistetForArbeidsgiver
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class SoknadService(
    val soknadRepository: SoknadRepository,
    val dialogportenService: DialogportenService,
) {
    fun hentSoknader(orgnr: String): List<Sykepengesoknad> =
        soknadRepository.hentSoknader(orgnr).map { it.whitelistetForArbeidsgiver().konverter() }

    fun hentSoknad(
        soknadId: UUID,
        orgnr: String,
    ): Sykepengesoknad? =
        soknadRepository.hentSoknad(soknadId)?.whitelistetForArbeidsgiver()?.konverter().takeIf {
            it?.arbeidsgiver?.orgnr ==
                orgnr
        }

    fun behandleSoknad(soknad: SykepengesoknadDTO) {
        if (!soknad.skalLagres()) {
            sikkerLogger().info("Søknad med id ${soknad.id} ignoreres fordi den ikke skal lagres eller sendes til arbeidsgiver.")
            return
        }

        if (soknad.erAlleredeLagret()) {
            sikkerLogger().info("Søknad med id ${soknad.id} ignoreres fordi den allerede er lagret.")
            return
        }

        try {
            val validertSoknad = soknad.validerPaakrevdeFelter()
            soknadRepository.lagreSoknad(validertSoknad)
            sikkerLogger().info("Lagret søknad med id: ${soknad.id}")

            if (soknad.skalSendesTilArbeidsgiver()) {
                dialogportenService.oppdaterDialogMedSykepengesoknad(
                    soknad =
                        DialogSykepengesoknad(
                            soknadId = validertSoknad.soknadId,
                            sykmeldingId = validertSoknad.sykmeldingId,
                            orgnr = Orgnr(validertSoknad.orgnr),
                        ),
                )
            } else {
                sikkerLogger().info(
                    "Sender _ikke_ søknad med søknadId: ${soknad.id}, sykmeldingId: ${soknad.sykmeldingId}, på orgnr: " +
                        "${soknad.arbeidsgiver?.orgnummer} videre til hag-dialog fordi den ikke skal sendes til arbeidsgiver.",
                )
            }
        } catch (e: IllegalArgumentException) {
            sikkerLogger().warn(
                "Ignorerer sykepengesøknad med id ${soknad.id} fordi søknaden mangler et påkrevd felt.",
                e,
            )
        }
    }

    private fun SykepengesoknadDTO.skalLagres(): Boolean =
        (
            erArbeidstakerSoknad() ||
                erArbeidstakerMedGradertReiseTilskudd() ||
                erArbeidstakerMedBehandlingsdager()

        ) &&
            !erEttersendtTilNAV() &&
            this.status == SykepengesoknadDTO.SoknadsstatusDTO.SENDT

    private fun SykepengesoknadDTO.skalSendesTilArbeidsgiver(): Boolean = this.sendtArbeidsgiver != null

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

    private fun SykepengesoknadDTO.erAlleredeLagret(): Boolean = soknadRepository.hentSoknad(id) != null

    private fun SykepengesoknadDTO.erEttersendtTilNAV() = sendtNav != null && sendtArbeidsgiver?.isBefore(sendtNav) ?: false
}
