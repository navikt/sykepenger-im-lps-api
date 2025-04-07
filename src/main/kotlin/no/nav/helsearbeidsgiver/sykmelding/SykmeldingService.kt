package no.nav.helsearbeidsgiver.sykmelding

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

    fun lagreSykmelding(sykmeldingMessage: SendSykmeldingAivenKafkaMessage): Pair<UUID, String>? {
        val id =
            sykmeldingMessage.sykmelding.id.toUuidOrNull()
                ?: throw IllegalArgumentException("SykmeldingId ${sykmeldingMessage.sykmelding.id} er ikke en gyldig UUID.")

        val orgnr =
            sykmeldingMessage.event.arbeidsgiver?.orgnummer
                ?: throw SykmeldingOrgnrManglerException("Lagret ikke sykmelding fordi den mangler orgnr $id")

        sikkerLogger().info("Lagrer sykmelding $id")

        when (sykmeldingRepository.hentSykmelding(id)) {
            null -> {
                sikkerLogger().info("Sykmelding $id er ny og vil derfor lagres.")
                sykmeldingRepository.lagreSykmelding(
                    id = id,
                    fnr = sykmeldingMessage.kafkaMetadata.fnr,
                    orgnr = orgnr,
                    sykmelding = sykmeldingMessage.sykmelding,
                )
                return id to orgnr
            }

            else -> {
                sikkerLogger().info("Sykmelding $id finnes fra før og vil derfor ignoreres.")
                return null
            }
        }
    }
}
