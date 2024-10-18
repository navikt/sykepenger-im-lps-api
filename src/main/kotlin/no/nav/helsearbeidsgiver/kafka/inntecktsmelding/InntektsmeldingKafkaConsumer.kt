package no.nav.helsearbeidsgiver.kafka.inntecktsmelding

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.inntektsmelding.ExposedMottak
import no.nav.helsearbeidsgiver.inntektsmelding.ImMottakRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import org.slf4j.LoggerFactory

class InntektsmeldingKafkaConsumer : LpsKafkaConsumer {
    private val logger = LoggerFactory.getLogger(InntektsmeldingKafkaConsumer::class.java)
    val jsonMapper =
        Json {
            ignoreUnknownKeys = true
        }

    override fun handleRecord(record: String) {
        // TODO: gjør dette i en transaksjon og gjør det skikkelig..
        logger.info("Received record: $record")
        try {
            val obj = jsonMapper.decodeFromString<RapidMessage>(record)
            println(obj.eventname)
            runBlocking {
                ImMottakRepository().opprett(ExposedMottak(record))
            }

            if (obj.eventname == "INNTEKTSMELDING_DISTRIBUERT") {
                runBlocking {
                    InntektsMeldingRepository().opprett(
                        im = jsonMapper.encodeToString(Inntektsmelding.serializer(), obj.inntektsmelding!!),
                        org = obj.inntektsmelding.avsender.orgnr.verdi,
                        sykmeldtFnr = obj.inntektsmelding.sykmeldt.fnr.verdi,
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to handle record", e)
        }
    }

    @Serializable
    data class RapidMessage(
        @SerialName("@event_name") val eventname: String,
        val inntektsmelding: Inntektsmelding? = null,
        val forespoersel: Forespoersel? = null,
    )
}
