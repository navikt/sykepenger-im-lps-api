package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class UnderkjentInntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    fun oppdaterInnteksmeldingStatusTilFeilet(inntektsmeldingId: UUID) {
        val antallRaderOppdatert =
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(inntektsmeldingId) // TODO: Legg til feilkode
        when {
            antallRaderOppdatert == 0 ->
                logger().error(
                    "Klarte ikke å oppdatere status på inntektsmelding $inntektsmeldingId til FEILET fordi den ikke finnes i databasen.",
                )

            antallRaderOppdatert > 1 ->
                logger().warn(
                    "Oppdaterte inntektsmelding $inntektsmeldingId til status FEILET. " +
                        "Oppdaterte et uventet antall rader: $antallRaderOppdatert.", // TODO: Ta med feilkode i loggen
                )

            else -> logger().info("Oppdaterte inntektsmelding $inntektsmeldingId til status FEILET.") // TODO: Ta med feilkode i loggen
        }
    }
}
