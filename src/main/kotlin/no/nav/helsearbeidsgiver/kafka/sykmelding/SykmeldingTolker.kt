package no.nav.helsearbeidsgiver.kafka.sykmelding

import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SykmeldingTolker(
    private val sykmeldingService: SykmeldingService,
    private val dialogportenService: IDialogportenService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()

    override fun lesMelding(melding: String) {
        try {
            val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)
            val lagretSykmeldingIdOrgnrPair = sykmeldingService.lagreSykmelding(sykmeldingMessage)
            sikkerLogger.info("Lagret sykmelding til database med id: ${sykmeldingMessage.sykmelding.id}")

            if (lagretSykmeldingIdOrgnrPair != null && unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding()) {
                val (sykmeldingId, orgnr) = lagretSykmeldingIdOrgnrPair
                dialogportenService.opprettNyDialogMedSykmelding(
                    orgnr = orgnr,
                    sykmeldingId = sykmeldingId,
                    sykmeldingMessage = sykmeldingMessage,
                )
            } else {
                sikkerLogger()
                    .info("Unleash toggle for å opprette Dialogporten dialog er skrudd av.")
            }
        } catch (e: Exception) {
            sikkerLogger.error("Klarte ikke å lagre sykmelding og opprette dialog!", e)
            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
        }
    }
}
