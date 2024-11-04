package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.db.Database.getForespoerselRepo
import no.nav.helsearbeidsgiver.db.Database.getMottakRepository
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.inntektsmelding.ExposedMottak
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.slf4j.LoggerFactory
import java.util.UUID

class InntektsmeldingKafkaConsumer : LpsKafkaConsumer {
    private val logger = LoggerFactory.getLogger(InntektsmeldingKafkaConsumer::class.java)
    private val inntektsmeldingService = InntektsmeldingService()
    val jsonMapper =
        Json {
            jsonConfig
            ignoreUnknownKeys = true
        }

    override fun handleRecord(record: String) {
        // TODO: gjør dette i en transaksjon og gjør det skikkelig..
        try {
            val obj = jsonMapper.decodeFromString<RapidMessage>(record)

            logger.info("Received event: ${obj.eventname}")
            getMottakRepository().opprett(ExposedMottak(record))

            if (obj.eventname == "INNTEKTSMELDING_DISTRIBUERT") {
                if (obj.inntektsmelding != null) {
                    inntektsmeldingService.opprettInntektsmelding(obj.inntektsmelding)
                } else {
                    logger.warn("Ugyldig event - mangler felt inntektsmelding, kan ikke lagre")
                }
            } else if (obj.eventname == "FORESPOERSEL_MOTTATT") {
                if (obj.behov == null) {
                    val forespoerselId =
                        obj.data
                            ?.get("forespoerselId")
                            ?.fromJson(UuidSerializer)
                            ?.toString()
                    val orgnr =
                        obj.data
                            ?.get("orgnrUnderenhet")
                            ?.fromJson(Orgnr.serializer())
                            ?.verdi
                    val fnr =
                        obj.data
                            ?.get("fnr")
                            ?.fromJson(Fnr.serializer())
                            ?.verdi

                    if (forespoerselId != null && orgnr != null && fnr != null) {
                        getForespoerselRepo().lagreForespoersel(
                            forespoerselId = forespoerselId.toString(),
                            organisasjonsnummer = orgnr.toString(),
                            foedselsnr = fnr,
                        )
                    } else {
                        logger.warn("Ugyldige verdier, kan ikke lagre!")
                    }
                }
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
        @SerialName("@behov") val behov: String? = null,
        val data: Map<String, JsonElement>? = emptyMap(),
        val inntektsmelding: Inntektsmelding? = null,
        val forespoersel: Forespoersel? = null,
        @Serializable(with = UuidSerializer::class) val forespoerselId: UUID? = null,
    )
}
