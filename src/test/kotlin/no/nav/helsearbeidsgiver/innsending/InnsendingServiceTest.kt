package no.nav.helsearbeidsgiver.innsending

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka.toJson
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockSkjemaInntektsmelding
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class InnsendingServiceTest {
    private val innsendingProducer = mockk<InnsendingProducer>()
    private val innsendingRepository = mockk<InnsendingRepository>()
    private val bakgrunnsjobbService = mockk<BakgrunnsjobbService>()
    private val innsendigService = InnsendingService(innsendingProducer, innsendingRepository, bakgrunnsjobbService)

    init {
        every {
            innsendingProducer.send(*anyVararg<Pair<InnsendingKafka.Key, JsonElement>>())
        } returns toResult("".toJson(String.serializer()))
    }

    private fun toResult(toJson: JsonElement): Result<JsonElement> = runCatching { toJson }

    @Test
    fun `sendInn kaller innsendingproducer sin send-metode med forventede nøkler og verdier`() {
        val innsendtSkjema = mockSkjemaInntektsmelding()

        val (kontekstId, mottatt) = innsendigService.sendInn(innsendtSkjema)

        verify {
            innsendingProducer.send(
                InnsendingKafka.Key.EVENT_NAME to InnsendingKafka.EventName.API_INNSENDING_STARTET.toJson(),
                InnsendingKafka.Key.KONTEKST_ID to kontekstId.toJson(UuidSerializer),
                InnsendingKafka.Key.DATA to
                    mapOf(
                        InnsendingKafka.Key.SKJEMA_INNTEKTSMELDING to innsendtSkjema.toJson(SkjemaInntektsmelding.serializer()),
                        InnsendingKafka.Key.MOTTATT to mottatt.toJson(LocalDateTimeSerializer),
                    ).toJson(),
            )
        }
    }

    @Test
    fun `lagreInnsending logger ved suksess`() {
        val innsendtSkjema = mockSkjemaInntektsmelding()
        val expectedUuid = UUID.randomUUID()

        every { innsendingRepository.opprettInnsending(any(), any(), any()) } returns expectedUuid

        innsendigService.lagreInnsending("orgnr", "lpsOrgnr", innsendtSkjema)

        verify {
            innsendingRepository.opprettInnsending("orgnr", "lpsOrgnr", innsendtSkjema)
        }
    }

    @Test
    fun `lagreInnsending kaster exception ved feil`() {
        val innsendtSkjema = mockSkjemaInntektsmelding()

        every { innsendingRepository.opprettInnsending(any(), any(), any()) } throws RuntimeException("Feil ved lagring")

        val exception =
            assertThrows<RuntimeException> {
                innsendigService.lagreInnsending("orgnr", "lpsOrgnr", innsendtSkjema)
            }
        assert(exception.message == "Feil ved lagring")
    }
}
