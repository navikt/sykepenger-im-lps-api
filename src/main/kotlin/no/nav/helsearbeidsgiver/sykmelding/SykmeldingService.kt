package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.sykmelding.model.Person
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmeldingArbeidsgiver
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
    ): Sykmelding? {
        try {
            val sykmeldingDTO = sykmeldingRepository.hentSykmelding(id).takeIf { it?.orgnr == orgnr }

            if (sykmeldingDTO == null) {
                logger().info("Fant ingen sykmeldinger $id for orgnr: $orgnr")
                return null
            }

            sikkerLogger().info("Hentet sykmelding $id for orgnr: $orgnr")

            val person = mockHentPersonFraPDL(sykmeldingDTO.fnr) // TODO: Bruk ekte PDL

            val sykmeldingArbeidsgiver = sykmeldingDTO.tilSykmeldingArbeidsgiver(person)

            return sykmeldingArbeidsgiver
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av sykmelding $id orgnr: $orgnr", e)
        }
    }

    fun lagreSykmelding(sykmeldingMessage: SendSykmeldingAivenKafkaMessage): Boolean {
        val id =
            sykmeldingMessage.sykmelding.id.toUuidOrNull()
                ?: throw IllegalArgumentException("SykmeldingId ${sykmeldingMessage.sykmelding.id} er ikke en gyldig UUID.")

        logger().info("Lagrer sykmelding $id")

        when (sykmeldingRepository.hentSykmelding(id)) {
            null -> {
                logger().info("Sykmelding $id er ny og vil derfor lagres.")
                sykmeldingRepository.lagreSykmelding(
                    id = id,
                    fnr = sykmeldingMessage.kafkaMetadata.fnr,
                    orgnr = sykmeldingMessage.event.arbeidsgiver.orgnummer,
                    sykmelding = sykmeldingMessage,
                )
                return true
            }

            else -> {
                logger().info("Sykmelding $id finnes fra før og vil derfor ignoreres.")
                return false
            }
        }
    }
}

// TODO: Implementer ekte PDL lookup
fun mockHentPersonFraPDL(fnr: String?): Person =
    Person(
        fornavn = "Ola",
        mellomnavn = null,
        etternavn = "Nordmann",
        aktorId = "aktorId",
        fnr = fnr ?: "Ingen fnr definert",
    )
