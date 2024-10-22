package no.nav.helsearbeidsgiver.kafka.inntecktsmelding

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.db.Database.getForespoerselRepo
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.inntektsmelding.ExposedMottak
import no.nav.helsearbeidsgiver.inntektsmelding.ImMottakRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsMeldingRepository
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import org.slf4j.LoggerFactory
import java.util.UUID

class InntektsmeldingKafkaConsumer : LpsKafkaConsumer {
    private val logger = LoggerFactory.getLogger(InntektsmeldingKafkaConsumer::class.java)
    val jsonMapper =
        Json {
            jsonConfig
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
            } else if (obj.eventname == "FORESPOERSEL_MOTTATT") {
                getForespoerselRepo().lagreForespoersel(
                    forespoerselId = obj.forespoerselId!!.toString(),
                    organisasjonsnummer = obj.orgnr!!.toString(),
                    foedselsnr = obj.fnr!!.toString(),
                )
            } else if (obj.eventname == "FORESPOERSEL_BESVART") {
                settBesvart(obj.forespoerselId.toString())
            } else if (obj.eventname == "FORESPOERSEL_FORKASTET") {
                settForkastet(obj.forespoerselId.toString())
            }
        } catch (e: Exception) {
            logger.error("Failed to handle record", e)
        }
    }

    private fun settForkastet(forespoerselId: String) {
        if (forespoerselId.isEmpty()) {
            logger.warn("ingen forespørselID")
        } else {
            val antall = getForespoerselRepo().settForkastet(forespoerselId)
            logger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status forkastet")
        }
    }

    private fun settBesvart(forespoerselId: String) {
        if (forespoerselId.isEmpty()) {
            logger.warn("ingen forespørselID")
        } else {
            val antall = getForespoerselRepo().settBesvart(forespoerselId)
            logger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status besvart")
        }
    }

    @Serializable
    data class RapidMessage(
        @SerialName("@event_name") val eventname: String,
        val inntektsmelding: Inntektsmelding? = null,
        val forespoersel: Forespoersel? = null,
        @Serializable(with = UuidSerializer::class) val forespoerselId: UUID? = null,
        @SerialName("identitetsnummer") val fnr: String? = null,
        @SerialName("orgnrUnderenhet") val orgnr: String? = null,
    )
}
