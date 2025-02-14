package no.nav.helsearbeidsgiver.innsending

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.Innsending.toJson
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InnsendingProducer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockSkjemaInntektsmelding
import org.junit.jupiter.api.Test

class InnsendingServiceTest {
    private val innsendingProducer = mockk<InnsendingProducer>()
    private val innsendigService = InnsendingService(innsendingProducer)

    init {
        every {
            innsendingProducer.send(*anyVararg<Pair<Innsending.Key, JsonElement>>())
        } returns toResult("".toJson(String.serializer()))
    }

    private fun toResult(toJson: JsonElement): Result<JsonElement> = runCatching { toJson }

    @Test
    fun `sendInn kaller innsendingproducer sin send-metode med forventede nøkler og verdier`() {
        val innsendtSkjema = mockSkjemaInntektsmelding()

        val (kontekstId, mottatt) = innsendigService.sendInn(innsendtSkjema)

        verify {
            innsendingProducer.send(
                Innsending.Key.EVENT_NAME to Innsending.EventName.API_INNSENDING_STARTET.toJson(),
                Innsending.Key.KONTEKST_ID to kontekstId.toJson(UuidSerializer),
                Innsending.Key.DATA to
                    mapOf(
                        Innsending.Key.SKJEMA_INNTEKTSMELDING to innsendtSkjema.toJson(SkjemaInntektsmelding.serializer()),
                        Innsending.Key.MOTTATT to mottatt.toJson(LocalDateTimeSerializer),
                    ).toJson(),
            )
        }
    }
}
