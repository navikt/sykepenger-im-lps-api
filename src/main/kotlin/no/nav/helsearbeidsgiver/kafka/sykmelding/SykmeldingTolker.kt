package no.nav.helsearbeidsgiver.kafka.sykmelding

import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.pdl.IPdlService
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull

private const val UKJENT_NAVN = "Ukjent navn"

class SykmeldingTolker(
    private val sykmeldingService: SykmeldingService,
    private val dialogportenService: IDialogportenService,
    private val pdlService: IPdlService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)
            val sykmeldtNavn = pdlService.hentPersonFulltNavnForSykmelding(sykmeldingMessage.kafkaMetadata.fnr) ?: UKJENT_NAVN
            val sykmeldingId =
                sykmeldingMessage.sykmelding.id.toUuidOrNull()
                    ?: throw IllegalArgumentException("Mottatt sykmeldingId ${sykmeldingMessage.sykmelding.id} er ikke en gyldig UUID.")

            val harLagretSykmelding = sykmeldingService.lagreSykmelding(sykmeldingMessage, sykmeldingId, sykmeldtNavn)

            logger.info("Lagret sykmelding til database med id: $sykmeldingId")

            if (harLagretSykmelding && unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding()) {
                dialogportenService.opprettNyDialogMedSykmelding(
                    orgnr = sykmeldingMessage.event.arbeidsgiver.orgnummer,
                    sykmeldingId = sykmeldingId,
                    sykmeldingMessage = sykmeldingMessage,
                )
                logger.info("Opprettet dialog for sykmelding $sykmeldingId")
            } else {
                logger
                    .info(
                        "Oppretter ikke dialog for sykmelding $sykmeldingId, enten fordi sykmeldingen ikke ble lagret eller fordi dialogopprettelse er skrudd av.",
                    )
            }
        } catch (e: Exception) {
            "Klarte ikke å lagre sykmelding og opprette Dialogporten-dialog!".also {
                logger.error(it)
                sikkerLogger.error(it, e)
            }
            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
        }
    }
}
