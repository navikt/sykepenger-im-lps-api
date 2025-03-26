package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.toUuidOrNull
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
        val id = sykmeldingMessage.sykmelding.id.toUuidOrNull()
        id ?: throw IllegalArgumentException("Sykmelding har ugyldig UUID ${sykmeldingMessage.sykmelding.id}")

        val orgnr = sykmeldingMessage.event.arbeidsgiver?.orgnummer
        orgnr ?: throw SykmeldingOrgnrManglerException("Lagret ikke sykmelding fordi den mangler orgnr $id")

        logger().info("Lagrer sykmelding $id")

        sykmeldingRepository.lagreSykmelding(
            id = id,
            fnr = sykmeldingMessage.kafkaMetadata.fnr,
            orgnr = orgnr,
            sykmelding = sykmeldingMessage.sykmelding,
        )
    }
}
