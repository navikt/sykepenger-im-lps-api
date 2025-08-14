package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmelding
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    fun hentSykmelding(id: UUID): Sykmelding? = sykmeldingRepository.hentSykmelding(id)?.tilSykmelding()

    @Deprecated(
        message =
            "Kan slettes når vi fjerner det utfasede endepunktet GET v1/sykmeldinger ." +
                "Bruk hentSykmeldinger(orgnr: String, filter: SykmeldingFilterRequest) istedenfor.",
    )
    fun hentSykmeldinger(orgnr: String): List<Sykmelding> =
        sykmeldingRepository.hentSykmeldinger(orgnr).map {
            it.tilSykmelding()
        }

    fun hentSykmeldinger(sykmeldingFilterRequest: SykmeldingFilterRequest): List<Sykmelding> =
        sykmeldingRepository.hentSykmeldinger(sykmeldingFilterRequest).map {
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
                    id = sykmeldingId,
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
