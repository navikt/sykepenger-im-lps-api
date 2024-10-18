package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.kafka.inntecktsmelding.InntektsmeldingKafkaConsumer
import org.slf4j.LoggerFactory

class InntektsmeldingService {
    private val logger = LoggerFactory.getLogger(InntektsmeldingKafkaConsumer::class.java)

    suspend fun hentInntektsmeldingerByOrgNr(orgnr: String): List<ExposedInntektsmelding> {
        runCatching {
            logger.info("Henter inntektsmeldinger for orgnr: $orgnr")
            InntektsMeldingRepository().hent(orgnr)
        }.onSuccess { return it }
            .onFailure { return emptyList() }

        return emptyList()
    }
}
