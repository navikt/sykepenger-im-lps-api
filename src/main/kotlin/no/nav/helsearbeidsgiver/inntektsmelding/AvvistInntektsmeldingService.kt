package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.utils.log.logger

class AvvistInntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val dokumentkoblingService: DokumentkoblingService,
) {
    private val logger = logger()

    fun oppdaterInnteksmeldingTilFeilet(avvistInntektsmelding: AvvistInntektsmelding) {
        when (
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(avvistInntektsmelding)
        ) {
            0 -> {
                logger.error(
                    "Klarte ikke å oppdatere inntektsmelding ${avvistInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${avvistInntektsmelding.feilkode} fordi den ikke finnes i databasen.",
                )
            }

            else -> {
                logger.info(
                    "Oppdaterte inntektsmelding ${avvistInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${avvistInntektsmelding.feilkode}.",
                )
                dokumentkoblingService.produserInntektsmeldingAvvistKobling(
                    avvistInntektsmelding,
                )
            }
        }
    }
}
