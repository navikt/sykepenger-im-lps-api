package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.dialogporten.DialogSykepengesoeknad
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.kapitaliserSykmeldtNavn
import no.nav.helsearbeidsgiver.utils.konverter
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.whitelistetForArbeidsgiver
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class SoeknadService(
    val soeknadRepository: SoeknadRepository,
    val sykmeldingService: SykmeldingService,
    val dialogportenService: DialogportenService,
    val dokumentkoblingService: DokumentkoblingService,
) {
    private val logger = logger()

    fun hentSoeknader(filter: SykepengesoeknadFilter): List<Sykepengesoeknad> =
        soeknadRepository
            .hentSoeknader(filter)
            .filter { it.sykepengeSoeknadKafkaMelding.skalSendesTilArbeidsgiver() }
            .map { it.sykepengeSoeknadKafkaMelding.whitelistetForArbeidsgiver().konverter(it.loepenr) }

    fun hentSoeknad(soeknadId: UUID): Sykepengesoeknad? =
        soeknadRepository
            .hentSoeknad(soeknadId)
            ?.takeIf { it.sykepengeSoeknadKafkaMelding.skalSendesTilArbeidsgiver() }
            ?.let { soeknad ->
                soeknad.sykepengeSoeknadKafkaMelding
                    .whitelistetForArbeidsgiver()
                    ?.konverter(soeknad.loepenr)
            }

    fun tilSoeknadForPdf(soeknad: Sykepengesoeknad): SykepengesoeknadForPDF {
        if (soeknad.sykmeldingId == null) {
            return SykepengesoeknadForPDF(soeknad, null)
        }
        val sykmelding = sykmeldingService.hentSykmelding(soeknad.sykmeldingId)
        if (sykmelding == null) {
            return SykepengesoeknadForPDF(soeknad, null)
        }
        val navn = sykmelding.kapitaliserSykmeldtNavn().sykmeldt.navn
        return SykepengesoeknadForPDF(soeknad, navn)
    }

    fun behandleSoeknad(soeknad: SykepengeSoeknadKafkaMelding) {
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

                dokumentkoblingService.produserSykepengesoeknadKobling(
                    soeknadId = validertSoeknad.soeknadId,
                    sykmeldingId = validertSoeknad.sykmeldingId,
                    orgnr = Orgnr(validertSoeknad.orgnr),
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

    private fun SykepengeSoeknadKafkaMelding.skalLagres(): Boolean =
        (
            erArbeidstakerSoeknad() ||
                erArbeidstakerMedGradertReiseTilskudd() ||
                erArbeidstakerMedBehandlingsdager()

        ) &&
            !erEttersendtTilNAV() &&
            this.status == SykepengeSoeknadKafkaMelding.SoknadsstatusDTO.SENDT

    private fun SykepengeSoeknadKafkaMelding.skalSendesTilArbeidsgiver(): Boolean = this.sendtArbeidsgiver != null

    private fun SykepengeSoeknadKafkaMelding.validerPaakrevdeFelter(): LagreSoeknad =
        LagreSoeknad(
            soeknadId = id,
            sykmeldingId = requireNotNull(sykmeldingId) { "SykmeldingId kan ikke være null" },
            fnr = fnr,
            orgnr = requireNotNull(arbeidsgiver?.orgnummer) { "Orgnummer kan ikke være null" },
            sykepengesoeknad = this,
        )

    private fun SykepengeSoeknadKafkaMelding.erArbeidstakerSoeknad(): Boolean =
        this.type == SykepengeSoeknadKafkaMelding.SoknadstypeDTO.ARBEIDSTAKERE

    private fun SykepengeSoeknadKafkaMelding.erArbeidstakerMedGradertReiseTilskudd(): Boolean =
        this.arbeidssituasjon == SykepengeSoeknadKafkaMelding.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengeSoeknadKafkaMelding.SoknadstypeDTO.GRADERT_REISETILSKUDD

    private fun SykepengeSoeknadKafkaMelding.erArbeidstakerMedBehandlingsdager(): Boolean =
        this.arbeidssituasjon == SykepengeSoeknadKafkaMelding.ArbeidssituasjonDTO.ARBEIDSTAKER &&
            this.type == SykepengeSoeknadKafkaMelding.SoknadstypeDTO.BEHANDLINGSDAGER

    private fun SykepengeSoeknadKafkaMelding.erAlleredeLagret(): Boolean = soeknadRepository.hentSoeknad(id) != null

    private fun SykepengeSoeknadKafkaMelding.erEttersendtTilNAV() = sendtNav != null && sendtArbeidsgiver?.isBefore(sendtNav) ?: false
}
