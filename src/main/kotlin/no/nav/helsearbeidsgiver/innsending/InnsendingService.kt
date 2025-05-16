package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.bakgrunnsjobb.LeaderElectedBakgrunnsjobbService
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.Innsending
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka.toJson
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.LocalDateTime
import java.util.UUID

class InnsendingService(
    private val innsendingProducer: InnsendingProducer,
    private val bakgrunnsjobbService: LeaderElectedBakgrunnsjobbService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) {
    fun lagreBakgrunsjobbInnsending(innsending: Innsending) {
        bakgrunnsjobbService.opprettJobb<InnsendingProcessor>(
            maksAntallForsoek = 10,
            data = innsending.toJson(Innsending.serializer()),
        )
    }

    fun sendInn(innsending: Innsending): Pair<UUID, LocalDateTime> {
        val mottatt = LocalDateTime.now()
        val kontekstId = UUID.randomUUID()

        if (unleashFeatureToggles.skalSendeApiInnsendteImerTilSimba()) {
            innsendingProducer
                .send(
                    InnsendingKafka.Key.EVENT_NAME to InnsendingKafka.EventName.API_INNSENDING_STARTET.toJson(),
                    InnsendingKafka.Key.KONTEKST_ID to kontekstId.toJson(UuidSerializer),
                    InnsendingKafka.Key.DATA to
                        mapOf(
                            InnsendingKafka.Key.INNSENDING to innsending.toJson(Innsending.serializer()),
                            InnsendingKafka.Key.MOTTATT to mottatt.toJson(LocalDateTimeSerializer),
                        ).toJson(),
                )
            sikkerLogger().info(
                "Sender melding om innsendt IM til innsending kafka topic med kontekstId: $kontekstId",
            )
        } else {
            sikkerLogger().info(
                "Sender _ikke_ melding om innsendt IM til innsending kafka topic.",
            )
        }

        return Pair(kontekstId, mottatt)
    }
}
