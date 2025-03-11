package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database

class SykmeldingRepository(
    private val db: Database,
) {
    private val sikkerLogger = sikkerLogger()

    fun opprettSykmelding(sykmeldingMessage: SendSykmeldingAivenKafkaMessage) {
        // TODO: Faktisk lagre sykmelding i database
        sikkerLogger.info("Lagrer sykmelding i database.")
    }
}
