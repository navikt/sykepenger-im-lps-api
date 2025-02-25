package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.Innsending.toJson
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
    private val innsendingRepository: InnsendingRepository,
) {
    fun lagreInnsending(
        organisasjonsNr: String,
        lpsOrgnr: String,
        skjema: SkjemaInntektsmelding,
    ): UUID =
        runCatching {
            innsendingRepository.opprettInnsending(organisasjonsNr, lpsOrgnr, skjema)
        }.onSuccess { uuid ->
            sikkerLogger().info("Innsending lagret med id: $uuid")
        }.onFailure { error ->
            sikkerLogger().error("Feilet ved lagring av innsending skjema med forspørselId = ${skjema.forespoerselId} ", error)
        }.getOrThrow()

    fun sendInn(skjema: SkjemaInntektsmelding): Pair<UUID, LocalDateTime> {
        val mottatt = LocalDateTime.now()
        val kontekstId = UUID.randomUUID()

        val publisert =
            innsendingProducer
                .send(
                    Innsending.Key.EVENT_NAME to Innsending.EventName.API_INNSENDING_STARTET.toJson(),
                    Innsending.Key.KONTEKST_ID to kontekstId.toJson(UuidSerializer),
                    Innsending.Key.DATA to
                        mapOf(
                            Innsending.Key.SKJEMA_INNTEKTSMELDING to skjema.toJson(SkjemaInntektsmelding.serializer()),
                            Innsending.Key.MOTTATT to mottatt.toJson(LocalDateTimeSerializer),
                        ).toJson(),
                ).getOrThrow()
        sikkerLogger().info("Publiserte melding om innsendt skjema:\n${publisert.toPretty()}")
        return Pair(kontekstId, mottatt)
    }
}
