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
    val oppdatertForespoerselId = UUID.randomUUID()

    val forespoerselMottattJson = buildForespoerselMottattJson(forespoerselId = forespoerselId)
    println(forespoerselMottattJson)
    val forespoerselOppdaterJson =
        buildForespoerselOppdatertJson(forespoerselId = oppdatertForespoerselId, eksponertForespoerselId = forespoerselId)
    println(forespoerselOppdaterJson)
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")
    val priRecord = ProducerRecord(priTopic, "key", forespoerselMottattJson)
    Producer.sendMelding(priRecord)
    val priRecordOppdatert = ProducerRecord(priTopic, "key", forespoerselOppdaterJson)
    Producer.sendMelding(priRecordOppdatert)
    Producer.kafkaProducer.close()
}

fun main() {
    sendNyforespoerselogOppdateringTilKafka()
}
