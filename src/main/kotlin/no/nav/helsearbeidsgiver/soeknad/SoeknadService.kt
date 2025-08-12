package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.dialogporten.DialogSykepengesoeknad
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.konverter
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.whitelistetForArbeidsgiver
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class SoeknadService(
    val soeknadRepository: SoeknadRepository,
    val dialogportenService: DialogportenService,
) {
    private val logger = logger()

    @Deprecated(
        message =
            "Kan slettes når vi fjerner det utfasede endepunktet GET v1/soeknader ." +
                "Bruk hentSoeknader(orgnr: String, filter: SykepengesoeknadFilter) istedenfor.",
    )
    fun hentSoeknader(orgnr: String): List<Sykepengesoeknad> =
        soeknadRepository.hentSoeknader(orgnr).map { it.whitelistetForArbeidsgiver().konverter() }

    fun hentSoeknader(
        orgnr: String,
        filter: SykepengesoeknadFilter,
    ): List<Sykepengesoeknad> = soeknadRepository.hentSoeknader(orgnr, filter).map { it.whitelistetForArbeidsgiver().konverter() }

    fun hentSoeknad(soeknadId: UUID): Sykepengesoeknad? =
        soeknadRepository.hentSoeknad(soeknadId)?.whitelistetForArbeidsgiver()?.konverter()

    fun behandleSoeknad(soeknad: SykepengesoknadDTO) {
        if (!soeknad.skalLagres()) {
            logger.info("Søknad med id ${soeknad.id} ignoreres fordi den ikke skal lagres eller sendes til arbeidsgiver.")
            return
        }

        if (soeknad.erAlleredeLagret()) {
            logger.info("Søknad med id ${soeknad.id} ignoreres fordi den allerede er lagret.")
            return
        }

        try {
            val validertSoeknad = soeknad.validerPaakrevdeFelter()
            soeknadRepository.lagreSoeknad(validertSoeknad)
            logger.info("Lagret søknad med id: ${soeknad.id}")

            if (soeknad.skalSendesTilArbeidsgiver()) {
                dialogportenService.oppdaterDialogMedSykepengesoeknad(
                    soeknad =
                        DialogSykepengesoeknad(
                            soeknadId = validertSoeknad.soeknadId,
                            sykmeldingId = validertSoeknad.sykmeldingId,
                            orgnr = Orgnr(validertSoeknad.orgnr),
                        ),
                )
            } else {
                logger.info(
                    "Sendte _ikke_ søknad med søknadId: ${validertSoeknad.soeknadId} og sykmeldingId: ${validertSoeknad.soeknadId} " +
                        "videre til hag-dialog fordi den ikke skal sendes til arbeidsgiver.",
                )
            }
        } catch (e: IllegalArgumentException) {
            "Ignorerer sykepengesøknad med id ${soeknad.id} fordi søknaden mangler et påkrevd felt.".also {
                logger.warn(it)
                sikkerLogger().warn(it, e)
            }
        }
    }

    private fun SykepengesoknadDTO.skalLagres(): Boolean =
        (
            erArbeidstakerSoeknad() ||
                erArbeidstakerMedGradertReiseTilskudd() ||
                erArbeidstakerMedBehandlingsdager()

        ) &&
            !erEttersendtTilNAV() &&
            this.status == SykepengesoknadDTO.SoknadsstatusDTO.SENDT

    private fun SykepengesoknadDTO.skalSendesTilArbeidsgiver(): Boolean = this.sendtArbeidsgiver != null

    private fun SykepengesoknadDTO.validerPaakrevdeFelter(): LagreSoeknad =
        LagreSoeknad(
            soeknadId = id,
            sykmeldingId = requireNotNull(sykmeldingId) { "SykmeldingId kan ikke være null" },
            fnr = fnr,
            orgnr = requireNotNull(arbeidsgiver?.orgnummer) { "Orgnummer kan ikke være null" },
            sykepengesoeknad = this,
        )

    private fun SykepengesoknadDTO.erArbeidstakerSoeknad(): Boolean = this.type == SykepengesoknadDTO.SoknadstypeDTO.ARBEIDSTAKERE

    private fun SykepengesoknadDTO.erArbeidstakerMedGradertReiseTilskudd(): Boolean =
        this.arbeidssituasjon == SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengesoknadDTO.SoknadstypeDTO.GRADERT_REISETILSKUDD

    private fun SykepengesoknadDTO.erArbeidstakerMedBehandlingsdager(): Boolean =
        this.arbeidssituasjon == SykepengesoknadDTO.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengesoknadDTO.SoknadstypeDTO.BEHANDLINGSDAGER

    private fun SykepengesoknadDTO.erAlleredeLagret(): Boolean = soeknadRepository.hentSoeknad(id) != null

    private fun SykepengesoknadDTO.erEttersendtTilNAV() = sendtNav != null && sendtArbeidsgiver?.isBefore(sendtNav) ?: false
}
