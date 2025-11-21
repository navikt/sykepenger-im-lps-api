package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class DialogportenService(
    val dialogProducer: DialogProducer,
    val soeknadRepository: SoeknadRepository,
    val inntektsmeldingRepository: InntektsmeldingRepository,
    val forespoerselRepository: ForespoerselRepository,
    val unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()

    fun opprettNyDialogMedSykmelding(sykmelding: DialogSykmelding) {
        if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(sykmelding.orgnr)) {
            dialogProducer.send(sykmelding)
            logger.info(
                "Sendte melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId}",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for sykmelding med sykmeldingId: ${sykmelding.sykmeldingId} fordi feature toggle er av.",
            )
        }
    }

    fun oppdaterDialogMedSykepengesoeknad(soeknad: DialogSykepengesoeknad) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(soeknad.orgnr)) {
            dialogProducer.send(soeknad)
            logger.info(
                "Sendte melding til hag-dialog for sykepengesøknad med søknadId: ${soeknad.soeknadId}, sykmeldingId: ${soeknad.sykmeldingId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for sykepengesøknad med søknadId: ${soeknad.soeknadId}, sykmeldingId: ${soeknad.sykmeldingId}, fordi feature toggle er av.",
            )
        }
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(forespoersel: ForespoerselDokument) {
        val sykmeldingId = hentSykmeldingId(forespoersel.vedtaksperiodeId)

        if (sykmeldingId == null) {
            logger.warn(
                "Fant ingen sykmeldinger for vedtaksperiodeId ${forespoersel.vedtaksperiodeId}. " +
                    "Kan derfor ikke produsere dialogmelding til hag-dialog.",
            )
            return
        }
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = Orgnr(forespoersel.orgnr))) {
            dialogProducer.send(
                DialogInntektsmeldingsforespoersel(
                    forespoerselId = forespoersel.forespoerselId,
                    sykmeldingId = sykmeldingId,
                    orgnr = Orgnr(forespoersel.orgnr),
                ),
            )

            logger.info(
                "Sendte melding til hag-dialog for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, sykmeldingId: $sykmeldingId.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, fordi feature toggle er av.",
            )
        }
    }

    fun oppdaterDialogMedInntektsmelding(
        inntektsmeldingId: UUID,
        arsakInnsending: AarsakInnsending? = AarsakInnsending.Ny,
    ) {
        val dialogInntektsmelding = inntektsmeldingRepository.hentInntektsmeldingDialogMelding(inntektsmeldingId)
        if (dialogInntektsmelding == null) {
            logger.warn(
                "Klarte ikke å finne alle data til dialogmelding for inntektsmelding med id: $inntektsmeldingId sender ikke melding til dialogporten.",
            )
            return
        }
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmelding(dialogInntektsmelding.orgnr)) {
            dialogProducer.send(dialogInntektsmelding)
            logger.info(
                "Sendte melding til hag-dialog for inntektsmelding med innsendingsId: ${dialogInntektsmelding.innsendingId}, sykmeldingId: ${dialogInntektsmelding.sykmeldingId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for inntektsmelding med innsendingsId: ${dialogInntektsmelding.innsendingId}, sykmeldingId: ${dialogInntektsmelding.sykmeldingId}, fordi feature toggle er av.",
            )
        }
    }

    fun oppdaterDialogMedUtgaattForespoersel(forespoersel: Forespoersel) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = Orgnr(forespoersel.orgnr))) {
            val utgaattForespoerselDialogMelding = forespoerselRepository.hentUtgaattForespoerselDialogMelding(forespoersel.navReferanseId)
            if (utgaattForespoerselDialogMelding == null) {
                logger.warn(
                    "Klarte ikke å finne alle data til dialogmelding for forespoersel med id: ${forespoersel.navReferanseId} sender ikke melding til dialogporten.",
                )
                return
            }

            dialogProducer.send(
                utgaattForespoerselDialogMelding,
            )

            logger.info(
                "Sendte melding til hag-dialog for utgått inntektsmeldingsforespørsel med id: ${forespoersel.navReferanseId}, sykmeldingId: ${utgaattForespoerselDialogMelding.sykmeldingId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for utgått inntektsmeldingsforespørsel med id: ${forespoersel.navReferanseId}, fordi feature toggle er av.",
            )
        }
    }

    private fun hentSykmeldingId(vedtaksperiodeId: UUID): UUID? {
        val sykmeldingIder =
            soeknadRepository
                .hentSoeknaderMedVedtaksperiodeId(vedtaksperiodeId)
                .sortedByDescending { soeknad ->
                    soeknad.sendtNav
                        ?: soeknad.sendtArbeidsgiver
                        ?: null.also { logger.warn("Sykepengesøknad ${soeknad.id} har hverken sendtNav eller sendtArbeidsgiver.") }
                }.mapNotNull { it.sykmeldingId }

        if (sykmeldingIder.toSet().size > 1) {
            logger.warn(
                "Fant ${sykmeldingIder.size} sykmeldinger med IDer $sykmeldingIder for " +
                    "vedtaksperiodeId $vedtaksperiodeId. Bruker den nyeste søknaden.",
            )
        }
        return sykmeldingIder.firstOrNull()
    }
}
