package no.nav.helsearbeidsgiver.kafka.sykmelding

import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class SykmeldingTolker(
    private val sykmeldingRepository: SykmeldingRepository,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()

    override fun lesMelding(melding: String) {
        val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)
        try {
            sykmeldingRepository.lagreSykmelding(sykmeldingMessage)
            sikkerLogger.error("Lagret sykmelding til database med id: ${sykmeldingMessage.sykmelding.id}")
        } catch (e: Exception) {
            sikkerLogger.error("Klarte ikke å lagre sykmelding i database!", e)
            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
        }
    }
}
