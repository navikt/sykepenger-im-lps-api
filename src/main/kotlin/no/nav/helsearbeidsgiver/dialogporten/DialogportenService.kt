package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

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
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = Orgnr(forespoersel.orgnr))) {
            val sykmeldingId = hentSykmeldingId(forespoersel.vedtaksperiodeId)

            if (sykmeldingId == null) {
                logger.warn(
                    "Fant ingen sykmeldinger for vedtaksperiodeId ${forespoersel.vedtaksperiodeId}. " +
                        "Kan derfor ikke produsere dialogmelding til hag-dialog.",
                )
                return
            }

            dialogProducer.send(
                DialogInntektsmeldingsforespoersel(
                    forespoerselId = forespoersel.forespoerselId,
                    sykmeldingId = sykmeldingId,
                    orgnr = Orgnr(forespoersel.orgnr),
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
