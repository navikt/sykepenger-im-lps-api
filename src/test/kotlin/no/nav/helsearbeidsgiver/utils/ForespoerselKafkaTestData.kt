package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

/**
 *  Denne filen genererer testdata for forespørsel og oppdatering av forespørsel i Kafka. Kun ment for lokal testing.
 */
fun sendNyforespoerselogOppdateringTilKafka() {
    val forespoerselId = UUID.randomUUID()

    val forespoerselMottattJson = buildForespoerselMottattJson(forespoerselId = forespoerselId, orgnummer = "311567470")
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")
    val priRecord = ProducerRecord(priTopic, "key", forespoerselMottattJson)
    Producer.sendMelding(priRecord)
    repeat(10) {
        val oppdatertForespoerselId = UUID.randomUUID()
        val forespoerselOppdaterJson =
            buildForespoerselOppdatertJson(
                forespoerselId = oppdatertForespoerselId,
                eksponertForespoerselId = forespoerselId,
                orgnummer = "311567470",
            )

        val priRecordOppdatert = ProducerRecord(priTopic, "key", forespoerselOppdaterJson)
        Producer.sendMelding(priRecordOppdatert)
    }
    Producer.kafkaProducer.close()
}

fun main() {
    sendNyforespoerselogOppdateringTilKafka()
    sendBesvart(UUID.fromString("d3b8f0c2-4c1e-4f5a-9b6e-7c8d9e0f1a2b"))
}

private fun sendBesvart(forespoerselId: UUID) {
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")
    val priRecordOppdatert =
        ProducerRecord(
            priTopic,
            "key",
            buildForspoerselBesvartMelding(forespoerselId),
        )
    Producer.sendMelding(priRecordOppdatert)
    Producer.kafkaProducer.close()
}
