package no.nav.helsearbeidsgiver.innsending

import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.bakgrunnsjobb.LeaderElectedBakgrunnsjobbService
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingKafka.toJson
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.json.toPretty
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.LocalDateTime
import java.util.UUID

class InnsendingService(
    private val innsendingProducer: InnsendingProducer,
    private val bakgrunnsjobbService: LeaderElectedBakgrunnsjobbService,
) {
    fun lagreBakgrunsjobbInnsending(skjema: SkjemaInntektsmelding) {
        bakgrunnsjobbService.opprettJobb<InnsendingProcessor>(
            maksAntallForsoek = 10,
            data = Json.encodeToString(SkjemaInntektsmelding.serializer(), skjema),
        )
    }

    fun sendInn(skjema: SkjemaInntektsmelding): Pair<UUID, LocalDateTime> {
        val mottatt = LocalDateTime.now()
        val kontekstId = UUID.randomUUID()

        val publisert =
            innsendingProducer
                .send(
                    InnsendingKafka.Key.EVENT_NAME to InnsendingKafka.EventName.API_INNSENDING_STARTET.toJson(),
                    InnsendingKafka.Key.KONTEKST_ID to kontekstId.toJson(UuidSerializer),
                    InnsendingKafka.Key.DATA to
                        mapOf(
                            InnsendingKafka.Key.SKJEMA_INNTEKTSMELDING to skjema.toJson(SkjemaInntektsmelding.serializer()),
                            InnsendingKafka.Key.MOTTATT to mottatt.toJson(LocalDateTimeSerializer),
                        ).toJson(),
                ).getOrThrow()
        sikkerLogger().info("Publiserte melding om innsendt skjema:\n${publisert.toPretty()}")
        return Pair(kontekstId, mottatt)
    }
}
