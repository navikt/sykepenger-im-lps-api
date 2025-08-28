package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.utils.log.logger

class UnderkjentInntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    private val logger = logger()

    fun oppdaterInnteksmeldingTilFeilet(underkjentInntektsmelding: UnderkjentInntektsmelding) {
        when (
            val antallRaderOppdatert = inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(underkjentInntektsmelding)
        ) {
            0 ->
                logger.error(
                    "Klarte ikke å oppdatere status på inntektsmelding ${underkjentInntektsmelding.inntektsmeldingId} til FEILET " +
                        "med feilkode ${underkjentInntektsmelding.feilkode} fordi den ikke finnes i databasen.",
                )

            1 ->
                logger.info(
                    "Oppdaterte inntektsmelding ${underkjentInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${underkjentInntektsmelding.feilkode}.",
                )

            else ->
                logger.warn(
                    "Oppdaterte inntektsmelding ${underkjentInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${underkjentInntektsmelding.feilkode}. Oppdaterte et uventet antall rader: $antallRaderOppdatert.",
                )
        }
    }
}
