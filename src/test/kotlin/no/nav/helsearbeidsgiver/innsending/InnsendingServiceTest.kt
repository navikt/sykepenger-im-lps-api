package no.nav.helsearbeidsgiver.innsending

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.bakgrunnsjobb.LeaderElectedBakgrunnsjobbService
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.Innsending
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka.toJson
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.getTestLeaderConfig
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockInnsending
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InnsendingServiceTest {
    private val innsendingProducer = mockk<InnsendingProducer>()
    private val bakgrunnsjobbRepository = mockk<BakgrunnsjobbRepository>(relaxed = true)
    private val leaderElectedBakgrunnsjobbService =
        LeaderElectedBakgrunnsjobbService(
            bakgrunnsjobbRepository,
            getTestLeaderConfig(isLeader = true),
        )
    private val mockUnleashFeatureToggles = mockk<UnleashFeatureToggles>()
    private val innsendingService =
        InnsendingService(innsendingProducer, leaderElectedBakgrunnsjobbService, mockUnleashFeatureToggles)

    @BeforeEach
    fun init() {
        clearAllMocks()
        every {
            innsendingProducer.send(any(), *anyVararg<Pair<InnsendingKafka.Key, JsonElement>>())
        } returns JsonNull
    }

    @Test
    fun `sendInn kaller innsendingproducer sin send-metode med forventede nøkler og verdier`() {
        every { mockUnleashFeatureToggles.skalSendeApiInnsendteImerTilSimba() } returns true

        val innsendtSkjema = mockInnsending()
        val (kontekstId, mottatt) = innsendingService.sendInn(innsendtSkjema)

        verify(exactly = 1) {
            innsendingProducer.send(
                innsendtSkjema.skjema.forespoerselId.toString(),
                InnsendingKafka.Key.EVENT_NAME to InnsendingKafka.EventName.API_INNSENDING_STARTET.toJson(),
                InnsendingKafka.Key.KONTEKST_ID to kontekstId.toJson(UuidSerializer),
                InnsendingKafka.Key.DATA to
                    mapOf(
                        InnsendingKafka.Key.INNSENDING to innsendtSkjema.toJson(Innsending.serializer()),
                        InnsendingKafka.Key.MOTTATT to mottatt.toJson(LocalDateTimeSerializer),
                    ).toJson(),
            )
        }
    }

    @Test
    fun `lagreBakgrunsjobbInnsending kaller bakgrunnsjobbService sin opprettJobb-metode med forventede parametere`() {
        val innsendtSkjema = mockInnsending()
        leaderElectedBakgrunnsjobbService.registrer(InnsendingProcessor(mockk()))
        innsendingService.lagreBakgrunsjobbInnsending(innsendtSkjema)

        verify {
            bakgrunnsjobbRepository.save(
                match { jobb ->
                    jobb.type == "innsendingsjobb" &&
                        jobb.maksAntallForsoek == 10 &&
                        jobb.dataJson == innsendtSkjema.toJson(Innsending.serializer())
                },
            )
        }
    }

    @Test
    fun `sendInn kaller _ikke_ innsendingproducer sin send-metode dersom videresending til simba er skrudd av med feature toggle`() {
        every { mockUnleashFeatureToggles.skalSendeApiInnsendteImerTilSimba() } returns false
        innsendingService.sendInn(mockInnsending())

        verify(exactly = 0) {
            innsendingProducer.send(any(), *anyVararg<Pair<InnsendingKafka.Key, JsonElement>>())
        }
    }
}
