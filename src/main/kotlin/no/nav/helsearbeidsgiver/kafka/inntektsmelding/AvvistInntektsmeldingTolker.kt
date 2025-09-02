package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.inntektsmelding.AvvistInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.AvvistInntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.fromJsonMapFiltered
import no.nav.helsearbeidsgiver.utils.json.parseJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class AvvistInntektsmeldingTolker(
    private val avvistInntektsmeldingService: AvvistInntektsmeldingService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val jsonMap = melding.parseJson().toMap()

            val eventName = InnsendingKafka.Key.EVENT_NAME.lesOrNull(InnsendingKafka.EventName.serializer(), jsonMap)

            if (eventName == InnsendingKafka.EventName.AVVIST_INNTEKTSMELDING) {
                val data = jsonMap[InnsendingKafka.Key.DATA]?.toMap().orEmpty()

                val avvistInntektsmelding =
                    InnsendingKafka.Key.AVVIST_INNTEKTSMELDING.lesOrNull(
                        AvvistInntektsmelding.serializer(),
                        data,
                    ) ?: throw IllegalArgumentException("Mangler avvist inntektsmelding i melding.")

                logger.info(
                    "Mottok melding om avvist inntektsmelding med id ${avvistInntektsmelding.inntektsmeldingId} med feilkode ${avvistInntektsmelding.feilkode}.",
                )

                avvistInntektsmeldingService.oppdaterInnteksmeldingTilFeilet(avvistInntektsmelding)
            }
        } catch (e: Exception) {
            val feilmelding = "Feilet under mottak av avvist inntektsmelding!"
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
