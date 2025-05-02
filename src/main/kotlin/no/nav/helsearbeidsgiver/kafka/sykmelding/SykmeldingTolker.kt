package no.nav.helsearbeidsgiver.kafka.sykmelding

import no.nav.helsearbeidsgiver.dialogporten.DialogSykmelding
import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.pdl.IPdlService
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

class SykmeldingTolker(
    private val sykmeldingService: SykmeldingService,
    private val dialogportenService: IDialogportenService,
    private val pdlService: IPdlService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        try {
            val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)
            val fullPerson = pdlService.hentFullPerson(sykmeldingMessage.kafkaMetadata.fnr)
            val sykmeldingId =
                sykmeldingMessage.sykmelding.id.toUuidOrNull()
                    ?: throw IllegalArgumentException("Mottatt sykmeldingId ${sykmeldingMessage.sykmelding.id} er ikke en gyldig UUID.")

            val harLagretSykmelding = sykmeldingService.lagreSykmelding(sykmeldingMessage, sykmeldingId, fullPerson.navn.fulltNavn())

            logger.info("Lagret sykmelding til database med id: $sykmeldingId")

            if (harLagretSykmelding) {
                val dialogSykmelding =
                    DialogSykmelding(
                        sykmeldingId = sykmeldingId,
                        orgnr = Orgnr(sykmeldingMessage.event.arbeidsgiver.orgnummer),
                        foedselsdato = fullPerson.foedselsdato,
                        fulltNavn = fullPerson.navn.fulltNavn(),
                        sykmeldingsperioder = sykmeldingMessage.sykmelding.sykmeldingsperioder.map { Periode(it.fom, it.tom) },
                    )
                dialogportenService.opprettNyDialogMedSykmelding(dialogSykmelding)
                logger.info("Opprettet dialog for sykmelding $sykmeldingId")
            } else {
                logger
                    .info(
                        "Oppretter ikke dialog for sykmelding $sykmeldingId, fordi sykmeldingen ikke ble lagret.",
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
