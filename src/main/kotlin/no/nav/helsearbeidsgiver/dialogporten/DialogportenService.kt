package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

class DialogportenService(
    val dialogProducer: DialogProducer,
    val soeknadRepository: SoeknadRepository,
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
        val orgnr = Orgnr(forespoersel.orgnr)
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr)) {
            val sykmeldingIder =
                soeknadRepository
                    .hentSoeknaderMedVedtaksperiodeId(forespoersel.vedtaksperiodeId)
                    .sortedByDescending { it.sendtArbeidsgiver }
                    .sortedByDescending { it.sendtNav }
                    .mapNotNull { it.sykmeldingId }

            val sykmeldingId =
                when {
                    sykmeldingIder.isEmpty() -> {
                        logger.warn(
                            "Fant ingen sykmeldinger for vedtaksperiodeId ${forespoersel.vedtaksperiodeId}. " +
                                "Kan derfor ikke produsere dialogmelding til hag-dialog.",
                        )
                        return
                    }

                    sykmeldingIder.toSet().size > 1 -> {
                        logger.warn(
                            "Fant ${sykmeldingIder.size} sykmeldinger med IDer $sykmeldingIder for " +
                                "vedtaksperiodeId ${forespoersel.vedtaksperiodeId}. Bruker den nyeste søknaden.",
                        )
                        sykmeldingIder.first()
                    }

                    else -> sykmeldingIder.first()
                }

            dialogProducer.send(
                DialogInntektsmeldingsforespoersel(
                    forespoerselId = forespoersel.forespoerselId,
                    sykmeldingId = sykmeldingId,
                    orgnr = orgnr,
                ),
            )

            logger.info(
                "Sendte melding til hag-dialog for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, sykmeldingId: ${forespoersel.forespoerselId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, på fordi feature toggle er av.",
            )
        }
    }
}
