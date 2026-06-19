package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmelding
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    fun hentSykmelding(id: UUID): Sykmelding? = hentInternSykmelding(id)?.tilSykmelding()

    // Midlertidig hack ifbm workaround:
    // Legger til denne metoden for å kunne hente sykmeldingKafkaMessage.
    // Denne metoden / DTO-objektet skal ikke brukes for ekstern visning og bør slettes når vi
    // fjerner workaround!
    fun hentInternSykmelding(id: UUID): SykmeldingDTO? = sykmeldingRepository.hentSykmelding(id)

    fun hentSykmeldinger(filter: SykmeldingFilter): List<Sykmelding> =
        sykmeldingRepository.hentSykmeldinger(filter).map {
            it.tilSykmelding()
        }

    fun lagreSykmelding(
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
        sykmeldingId: UUID,
        sykmeldtNavn: String,
    ): Boolean {
        logger().info("Lagrer sykmelding $sykmeldingId")

        when (sykmeldingRepository.hentSykmelding(sykmeldingId)) {
            null -> {
                logger().info("Sykmelding $sykmeldingId er ny og vil derfor lagres.")
                sykmeldingRepository.lagreSykmelding(
                    sykmeldingId = sykmeldingId,
                    fnr = sykmeldingMessage.kafkaMetadata.fnr,
                    orgnr = sykmeldingMessage.event.arbeidsgiver.orgnummer,
                    sykmelding = sykmeldingMessage,
                    sykmeldtNavn = sykmeldtNavn,
                )
                return true
            }

            else -> {
                logger().warn("Sykmelding $sykmeldingId finnes fra før og vil derfor ignoreres.")
                return false
            }
        }
    }
}
