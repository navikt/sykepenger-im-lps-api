package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.util.UUID

class SykmeldingOrgnrManglerException(
    feilmelding: String,
) : RuntimeException(feilmelding)

class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    fun hentSykmelding(
        id: UUID,
        orgnr: String,
    ): SykmeldingResponse? {
        try {
            val response = sykmeldingRepository.hentSykmelding(id).takeIf { it?.orgnr == orgnr }
            if (response != null) {
                sikkerLogger().info("Hentet sykmelding $id for orgnr: $orgnr")
            } else {
                sikkerLogger().info("Fant ingen sykmeldinger $id for orgnr: $orgnr")
            }
            return response
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av sykmelding $id orgnr: $orgnr", e)
        }
    }

    fun lagreSykmelding(sykmeldingMessage: SendSykmeldingAivenKafkaMessage) {
        try {
            val a =
                sykmeldingMessage.sykmelding.id
                    .runCatching(UUID::fromString)
                    ?.getOrNull()
            val id = UUID.fromString(sykmeldingMessage.sykmelding.id).also { logger().info("Lagrer sykmelding $it.") }
            val orgnr = sykmeldingMessage.event.arbeidsgiver?.orgnummer

            if (orgnr == null) {
                val feilmelding = "Lagret ikke sykmelding fordi den mangler orgnr [id: $id]".also { logger().error(it) }
                throw SykmeldingOrgnrManglerException(feilmelding)
            }

            sykmeldingRepository.lagreSykmelding(
                id = UUID.fromString(sykmeldingMessage.sykmelding.id),
                fnr = sykmeldingMessage.kafkaMetadata.fnr,
                orgnr = orgnr,
                sykmelding = sykmeldingMessage.sykmelding,
            )
        } catch (e: ExposedSQLException) {
            sikkerLogger().warn("SQL feil ved lagring av sykmelding [id: ${sykmeldingMessage.sykmelding.id}] message:${e.message}")
            throw e
        }
    }
}
