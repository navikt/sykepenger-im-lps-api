package no.nav.helsearbeidsgiver.innsending

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.bakgrunnsjobb.LeaderElectedBakgrunnsjobbService
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka.toJson
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockSkjemaInntektsmelding
import org.junit.jupiter.api.Test

class InnsendingServiceTest {
    private val innsendingProducer = mockk<InnsendingProducer>()
    private val bakgrunnsjobbRepository = mockk<BakgrunnsjobbRepository>(relaxed = true)
    private val leaderElectedBakgrunnsjobbService =
        LeaderElectedBakgrunnsjobbService(
            bakgrunnsjobbRepository,
            httpClient = createHttpClient(),
        )
    private val innsendigService = InnsendingService(innsendingProducer, leaderElectedBakgrunnsjobbService)

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
    fun `lagreBakgrunsjobbInnsending kaller bakgrunnsjobbService sin opprettJobb-metode med forventede parametere`() {
        val innsendtSkjema = mockSkjemaInntektsmelding()
        leaderElectedBakgrunnsjobbService.registrer(InnsendingProcessor(mockk()))
        innsendigService.lagreBakgrunsjobbInnsending(innsendtSkjema)

        verify {
            bakgrunnsjobbRepository.save(
                match { jobb ->
                    jobb.type == "innsendingsjobb" &&
                        jobb.maksAntallForsoek == 10 &&
                        jobb.data == Json.encodeToString(SkjemaInntektsmelding.serializer(), innsendtSkjema)
                },
            )
        }
    }
}
