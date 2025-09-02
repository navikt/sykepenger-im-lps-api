package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.utils.log.logger

class AvvistInntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    private val logger = logger()

    fun oppdaterInnteksmeldingTilFeilet(avvistInntektsmelding: AvvistInntektsmelding) {
        when (
            val antallRaderOppdatert = inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(avvistInntektsmelding)
        ) {
            0 ->
                logger.error(
                    "Klarte ikke å oppdatere inntektsmelding ${avvistInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${avvistInntektsmelding.feilkode} fordi den ikke finnes i databasen.",
                )

            1 ->
                logger.info(
                    "Oppdaterte inntektsmelding ${avvistInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${avvistInntektsmelding.feilkode}.",
                )

            else ->
                logger.warn(
                    "Oppdaterte inntektsmelding ${avvistInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${avvistInntektsmelding.feilkode}. Oppdaterte et uventet antall rader: $antallRaderOppdatert.",
                )
        }
    }
}
