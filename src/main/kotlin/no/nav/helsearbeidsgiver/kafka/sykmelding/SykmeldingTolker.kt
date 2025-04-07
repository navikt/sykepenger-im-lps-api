package no.nav.helsearbeidsgiver.kafka.sykmelding

import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SykmeldingTolker(
    private val sykmeldingService: SykmeldingService,
    private val dialogportenService: IDialogportenService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()

    override fun lesMelding(melding: String) {
        try {
            sikkerLogger.info("Gjør klar for lagring av mottatt sykmelding.")
            val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)
            val (sykmeldingId, orgnr) = sykmeldingService.lagreSykmelding(sykmeldingMessage = sykmeldingMessage)
            sikkerLogger.info("Lagret sykmelding til database med id: ${sykmeldingMessage.sykmelding.id}.")

            if (false && orgnr == "315587336") {
                dialogportenService.opprettNyDialogMedSykmelding(
                    orgnr = orgnr,
                    sykmeldingId = sykmeldingId,
                )
                sikkerLogger.info("Opprettet dialog i Dialogporten med sykmelding for orgnr: $orgnr og sykmeldingId: $sykmeldingId.")
            }
        } catch (e: Exception) {
            sikkerLogger.error("Klarte ikke å lagre og opprette dialog for sykmelding!", e)
            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
        }
    }
}
