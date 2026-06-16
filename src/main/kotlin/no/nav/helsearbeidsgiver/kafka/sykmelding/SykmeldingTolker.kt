package no.nav.helsearbeidsgiver.kafka.sykmelding

import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.pdl.FantIkkePersonException
import no.nav.helsearbeidsgiver.pdl.PdlService
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
import org.jetbrains.exposed.sql.transactions.transaction

class SykmeldingTolker(
    private val sykmeldingService: SykmeldingService,
    private val dokumentkoblingService: DokumentkoblingService,
    private val pdlService: PdlService,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()
    private val logger = logger()

    override fun lesMelding(melding: String) {
        transaction {
            try {
                val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)
                val sykmeldingId =
                    sykmeldingMessage.sykmelding.id.toUuidOrNull()
                        ?: throw IllegalArgumentException("Mottatt sykmeldingId ${sykmeldingMessage.sykmelding.id} er ikke en gyldig UUID.")

                val fullPerson = pdlService.hentFullPerson(sykmeldingMessage.kafkaMetadata.fnr, sykmeldingId)

                sykmeldingService.lagreSykmelding(sykmeldingMessage, sykmeldingId, fullPerson.navn.fulltNavn())
                dokumentkoblingService.produserSykmeldingKobling(
                    sykmeldingId = sykmeldingId,
                    sykmeldingMessage = sykmeldingMessage,
                    fullPerson = fullPerson,
                )
            } catch (e: FantIkkePersonException) {
                logger.error("Fant ikke person i PDL, ignorerer sykmelding!")
                sikkerLogger.error(
                    "Fant ikke person i PDL med fnr(${e.fnr}), ignorerer sykmelding med id: ${e.sykmeldingId}!",
                    e,
                )
            } catch (e: Exception) {
                // TODO: Skille på andre exceptions fra pdl, fra db og fra kafka
                "En feil oppstod, avbryter og forsøker igjen".also {
                    logger.error(it)
                    sikkerLogger.error(it, e)
                }
                rollback()
                throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
            }
        }
    }
}
