package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.EventName
import no.nav.helsearbeidsgiver.kafka.LpsKafkaConsumer
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.slf4j.LoggerFactory
import java.util.UUID

class SimbaKafkaConsumer(
    private val inntektsmeldingService: InntektsmeldingService,
    private val forespoerselRepository: ForespoerselRepository,
    private val mottakRepository: MottakRepository,
) : LpsKafkaConsumer {
    private val logger = LoggerFactory.getLogger(SimbaKafkaConsumer::class.java)
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    override fun handleRecord(record: String) {
        // TODO: gjør dette i en transaksjon og gjør det skikkelig..
        // transaction {
        val obj = parseRecord(record)
        if (obj == null) {
            sikkerLogger.warn("Ugyldig event mottatt: $record")
            mottakRepository.opprett(ExposedMottak(inntektsMelding = record, gyldig = false))
            return
        }
        try {
            logger.info("Received event: ${obj.eventname}")

            when (obj.eventname) {
                EventName.INNTEKTSMELDING_DISTRIBUERT.toString() -> {
                    if (obj.inntektsmelding != null) {
                        inntektsmeldingService.opprettInntektsmelding(obj.inntektsmelding)
                        mottakRepository.opprett(ExposedMottak(record))
                    } else {
                        logger.warn("Ugyldig event - mangler felt inntektsmelding, kan ikke lagre")
                    }
                }
                EventName.FORESPOERSEL_MOTTATT.toString() -> {
                    when (obj.behov) {
                        null -> {
                            val forespoerselId =
                                obj.data
                                    ?.get("forespoerselId")
                                    ?.fromJson(UuidSerializer)
                                    ?.toString()
                            val forespoerselPayload = obj.data?.get("forespoersel")?.fromJson(ForespoerselDokument.serializer())

                            val orgnr = forespoerselPayload?.orgnr
                            val fnr = forespoerselPayload?.fnr
//                                obj.data
//                                    ?.get("orgnrUnderenhet")
//                                    ?.fromJson(Orgnr.serializer())
//                                    ?.verdi
//                            val fnr =
//                                obj.data
//                                    ?.get("fnr")
//                                    ?.fromJson(Fnr.serializer())
//                                    ?.verdi
                            if (forespoerselId != null && orgnr != null && fnr != null) {
                                forespoerselRepository.lagreForespoersel(
                                    forespoerselId = forespoerselId.toString(),
                                    payload = forespoerselPayload,
                                )
                                mottakRepository.opprett(ExposedMottak(record))
                            } else {
                                logger.warn("Ugyldige verdier, kan ikke lagre!")
                            }
                        }
                    }
                }
                EventName.FORESPOERSEL_BESVART.toString() -> {
                    settBesvart(obj.forespoerselId.toString())
                    mottakRepository.opprett(ExposedMottak(record))
                }
                EventName.FORESPOERSEL_FORKASTET.toString() -> {
                    settForkastet(obj.forespoerselId.toString())
                    mottakRepository.opprett(ExposedMottak(record))
                }
            }
        } catch (e: Exception) {
            logger.warn("feil - $e")
        }
    }

    private fun parseRecord(record: String): RapidMessage? {
        try {
            return jsonMapper.decodeFromString<RapidMessage>(record)
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to handle record", e)
            return null
        }
    }

    private fun settForkastet(forespoerselId: String) {
        if (forespoerselId.isEmpty()) {
            logger.warn("ingen forespørselID")
        } else {
            val antall = forespoerselRepository.settForkastet(forespoerselId)
            logger.info("Oppdaterte $antall forespørsel med id $forespoerselId til status forkastet")
        }
    }

    private fun settBesvart(forespoerselId: String) {
        if (forespoerselId.isEmpty()) {
            logger.warn("ingen forespørselID")
        } else {
            val antall = forespoerselRepository.settBesvart(forespoerselId)
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
