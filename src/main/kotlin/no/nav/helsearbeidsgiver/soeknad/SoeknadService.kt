package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.pdl.PdlService
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.kapitaliserSykmeldtNavn
import no.nav.helsearbeidsgiver.utils.konverter
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.whitelistetForArbeidsgiver
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDateTime
import java.util.UUID

class SoeknadService(
    val soeknadRepository: SoeknadRepository,
    val sykmeldingService: SykmeldingService,
    val dokumentkoblingService: DokumentkoblingService,
    val pdlService: PdlService,
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

    fun behandleEttersendtSoeknad(soeknad: SykepengeSoeknadKafkaMelding) {
        if (!soeknad.skalLagres()) {
            logger.debug("Dry run: Søknad med id ${soeknad.id} ignoreres fordi den ikke skal lagres.")
            return
        }
        if (!soeknad.skalSendesTilArbeidsgiver()) return
        try {
            if (soeknad.sendtArbeidsgiver != null && soeknad.sendtArbeidsgiver.isAfter(LocalDateTime.of(2026, 6, 24, 10, 30))) {
                logger.info("Dry run(FERDIG): Søknad med id ${soeknad.id} ignoreres fordi den er ettersendt til NAV før 24.06.2026.")
                return
            }
            val validertSoeknad = soeknad.validerPaakrevdeFelter()
            val eksisterendeSoeknad = soeknadRepository.hentSoeknad(soeknad.id)
            if (eksisterendeSoeknad != null) {
                if (soeknad.skalErstattEksisterende(eksisterendeSoeknad)) {
                    // soeknadRepository.erstattSoeknad(validertSoeknad)

                    logger.info("Dry run: Erstattet søknad med id: ${soeknad.id} fordi den er ettersendt til NAV.")
                } else {
                    logger.info("Dry run: Søknad med id ${soeknad.id} er allerede lagret med sendtTilArbeidsgiver.")
                }
            } else {
                logger.warn("Dry run: Ettersendt søknad med id: ${soeknad.id} finnes ikke i databasen.")
            }
        } catch (e: IllegalArgumentException) {
            "Dry run: Ignorerer ettersendt sykepengesøknad med id ${soeknad.id} fordi søknaden mangler et påkrevd felt.".also {
                logger.warn(it)
                sikkerLogger().warn(it, e)
            }
        }
    }

    fun behandleSoeknad(soeknad: SykepengeSoeknadKafkaMelding) {
        if (!soeknad.skalLagres()) {
            logger.info("Søknad med id ${soeknad.id} ignoreres fordi den ikke skal lagres eller sendes til arbeidsgiver.")
            return
        }
        try {
            val validertSoeknad = soeknad.validerPaakrevdeFelter()
            val eksisterendeSoeknad = soeknadRepository.hentSoeknad(soeknad.id)
            if (eksisterendeSoeknad != null) {
                if (soeknad.skalErstattEksisterende(eksisterendeSoeknad)) {
                    soeknadRepository.erstattSoeknad(validertSoeknad)
                    logger.info("Erstattet søknad med id: ${soeknad.id} fordi sendtArbeidsgiver er oppdatert.")
                } else {
                    logger.info("Søknad med id ${soeknad.id} er allerede lagret.")
                }
            } else {
                soeknadRepository.lagreSoeknad(validertSoeknad)
                logger.info("Lagret søknad med id: ${soeknad.id}")
            }

            if (soeknad.skalSendesTilArbeidsgiver()) {
                slaaOppOgSendTilhoerendeSykmeldingTilDialog(validertSoeknad)

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

    // Vi mangler noen sykmeldinger i dialog-appen, for disse tilfellene får vi ikke sendt ut søknad.
    // WORKAROUND: Send sykmelding også, hvis vi har fått den! Kan sikkert fjernes 01.01.2027, eller enda tidligere
    private fun slaaOppOgSendTilhoerendeSykmeldingTilDialog(validertSoeknad: LagreSoeknad) {
        try {
            val sykmeldingMedOrginalMelding = sykmeldingService.hentInternSykmelding(validertSoeknad.sykmeldingId)

            sykmeldingMedOrginalMelding?.let {
                val fullPerson = pdlService.hentFullPerson(validertSoeknad.fnr, validertSoeknad.sykmeldingId)
                dokumentkoblingService.produserSykmeldingKobling(
                    sykmeldingId = validertSoeknad.sykmeldingId,
                    sykmeldingMessage = sykmeldingMedOrginalMelding.sendSykmeldingAivenKafkaMessage,
                    fullPerson = fullPerson,
                )
            }
        } catch (e: Exception) {
            sikkerLogger().warn("Klarte ikke å slå opp sykmeldingdata for sykmelding: ${validertSoeknad.sykmeldingId}", e)
            // Ikke sikkert at vi har mottatt sykmeldingen enda, og PDL-oppslaget kan feile
            // - ikke veldig kritisk, så bare logger det og går videre uten å sende sykmelding i disse tilfellene.
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

    private fun SykepengeSoeknadKafkaMelding.skalErstattEksisterende(eksisterende: SykepengeSoeknadDto): Boolean =
        this.sendtArbeidsgiver != null && eksisterende.sykepengeSoeknadKafkaMelding.sendtArbeidsgiver == null

    private fun SykepengeSoeknadKafkaMelding.erEttersendtTilNAV() = sendtNav != null && sendtArbeidsgiver?.isBefore(sendtNav) ?: false
}
