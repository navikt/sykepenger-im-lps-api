package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import no.nav.helsearbeidsgiver.inntektsmelding.UnderkjentInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.UnderkjentInntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.fromJsonMapFiltered
import no.nav.helsearbeidsgiver.utils.json.parseJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class UnderkjentInntektsmeldingTolker(
    private val underkjentInntektsmeldingService: UnderkjentInntektsmeldingService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val jsonMap = melding.parseJson().toMap().filterValues { it !is JsonNull }

            val eventName = InnsendingKafka.Key.EVENT_NAME.lesOrNull(InnsendingKafka.EventName.serializer(), jsonMap)

            if (eventName == InnsendingKafka.EventName.UNDERKJENT_INNTEKTSMELDING) {
                val data = jsonMap[InnsendingKafka.Key.DATA]?.toMap().orEmpty()

                val underkjentInntektsmelding =
                    InnsendingKafka.Key.UNDERKJENT_INNTEKTSMELDING.lesOrNull(
                        UnderkjentInntektsmelding.serializer(),
                        data,
                    ) ?: throw IllegalArgumentException("Mangler underkjent inntektsmelding i melding.")

                logger.info(
                    "Mottok melding om underkjent inntektsmelding med id ${underkjentInntektsmelding.inntektsmeldingId} med feilkode ${underkjentInntektsmelding.feilkode}.",
                )

                underkjentInntektsmeldingService.oppdaterInnteksmeldingTilFeilet(underkjentInntektsmelding)
            }
        } catch (e: Exception) {
            val feilmelding = "Feilet under mottak av underkjent inntektsmelding!"
            logger.error(feilmelding)
            sikkerLogger.error(feilmelding, e)
            throw e
        }
    }

    private fun JsonElement.toMap(): Map<InnsendingKafka.Key, JsonElement> = fromJsonMapFiltered(InnsendingKafka.Key.serializer())

    private fun <K : InnsendingKafka.Key, T : Any> K.lesOrNull(
        serializer: KSerializer<T>,
        melding: Map<K, JsonElement>,
    ): T? = melding[this]?.fromJson(serializer.nullable)
}
