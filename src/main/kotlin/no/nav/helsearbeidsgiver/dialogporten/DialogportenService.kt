package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID
import no.nav.helsearbeidsgiver.domene.forespoersel.ForespoerselFraBro as Forespoersel

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

    fun oppdaterDialogMedInntektsmeldingsforespoersel(forespoersel: Forespoersel) {
        val sykmeldingId = hentSykmeldingId(forespoersel.vedtaksperiodeId)

        if (sykmeldingId == null) {
            logger.warn(
                "Fant ingen sykmeldinger for vedtaksperiodeId ${forespoersel.vedtaksperiodeId}. " +
                    "Kan derfor ikke produsere dialogmelding til hag-dialog.",
            )
            return
        }
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = forespoersel.orgnr)) {
            dialogProducer.send(
                DialogInntektsmeldingsforespoersel(
                    forespoerselId = forespoersel.forespoerselId,
                    sykmeldingId = sykmeldingId,
                    orgnr = forespoersel.orgnr,
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

    fun oppdaterDialogMedInntektsmelding(inntektsmeldingId: UUID) {
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

    fun oppdaterDialogMedUtgaattForespoersel(
        forespoerselId: UUID,
        orgnr: Orgnr,
    ) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = orgnr)) {
            val utgaattForespoerselDialogMelding = forespoerselRepository.hentUtgaattForespoerselDialogMelding(forespoerselId)
            if (utgaattForespoerselDialogMelding == null) {
                logger.warn(
                    "Klarte ikke å finne alle data til dialogmelding for forespoersel med id: $forespoerselId sender ikke melding til dialogporten.",
                )
                return
            }

            dialogProducer.send(
                utgaattForespoerselDialogMelding,
            )

            logger.info(
                "Sendte melding til hag-dialog for utgått inntektsmeldingsforespørsel med id: $forespoerselId, sykmeldingId: ${utgaattForespoerselDialogMelding.sykmeldingId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for utgått inntektsmeldingsforespørsel med id: $forespoerselId, fordi feature toggle er av.",
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
