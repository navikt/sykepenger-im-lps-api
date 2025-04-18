package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.sykmelding.model.Person
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmeldingArbeidsgiver
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
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

// TODO: Implementer ekte PDL lookup
fun mockHentPersonFraPDL(fnr: String?): Person =
    Person(
        fornavn = "Ola",
        mellomnavn = null,
        etternavn = "Nordmann",
        aktorId = "aktorId",
        fnr = fnr ?: "Ingen fnr definert",
    )
