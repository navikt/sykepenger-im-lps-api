package no.nav.helsearbeidsgiver.kafka.sykmelding

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SykmeldingTolker(
    private val sykmeldingService: SykmeldingService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()

    override fun lesMelding(melding: String) {
        try {
            val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)

            runBlocking {
                val sykmeldtNavn = "abcd"

                sykmeldingService.lagreSykmelding(sykmeldingMessage, sykmeldtNavn)
                sikkerLogger.error("Lagret sykmelding til database med id: ${sykmeldingMessage.sykmelding.id}")

                if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding()) {
                    sikkerLogger()
                        .info("Unleash toggle for å opprette Dialogporten dialog er skrudd på (dialogopprettelse ikke implementert ennå).")
                } else {
                    sikkerLogger()
                        .info("Unleash toggle for å opprette Dialogporten dialog er skrudd av.")
                }
            }
        } catch (e: Exception) {
            sikkerLogger.error("Klarte ikke å lagre sykmelding i database!", e)
            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
        }
    }
}
