package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import no.nav.helsearbeidsgiver.inntektsmelding.UnderkjentInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.UnderkjentInntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class UnderkjentInntektsmeldingTolker(
    private val underkjentInntektsmeldingService: UnderkjentInntektsmeldingService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val underkjentInntektsmelding = melding.fromJson(UnderkjentInntektsmelding.serializer())
            logger.info(
                // TODO: Fullfør loggmelding
                "Mottok melding om underkjent inntektsmelding med id ${underkjentInntektsmelding.inntektsmeldingId} av typen .",
            )
            underkjentInntektsmeldingService.oppdaterInnteksmeldingStatusTilFeilet(underkjentInntektsmelding.inntektsmeldingId)
        } catch (e: Exception) {
            val errorMsg = "Feilet under mottak av underkjent inntektsmelding!"
            logger.error(errorMsg)
            sikkerLogger.error(errorMsg, e)
            throw e
        }
    }
}
