package no.nav.helsearbeidsgiver.refusjon

import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class RefusjonUtfallService(
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val refusjonKlient: RefusjonKlient,
    private val refusjonUtfallRepository: RefusjonUtfallRepository,
) {
    private val logger = logger()

    fun hentRefusjonUtfall(refusjonUtfallId: UUID): RefusjonUtfallResponse? = refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId)

    fun hentRefusjonUtfall(filter: RefusjonUtfallFilter): List<RefusjonUtfallResponse> = refusjonUtfallRepository.hentRefusjonUtfall(filter)

    suspend fun hentRefusjonUtfallPdf(refusjonUtfallId: UUID): ByteArray? = refusjonKlient.hentRefusjonUtfallPdf(refusjonUtfallId)

    fun sendVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        if (unleashFeatureToggles.skalLagreVedtakArbeidsgiver()) {
            refusjonKlient.sendVedtak(vedtakArbeidsgiverMelding)
        } else {
            logger.info(
                "Sender _ikke_ vedtak for vedtaksperiodeId ${vedtakArbeidsgiverMelding.vedtaksperiodeId} til refusjon fordi " +
                    "featuretoggle lagre-vedtak-arbeidsgiver er skrudd av.",
            )
        }
    }
}
