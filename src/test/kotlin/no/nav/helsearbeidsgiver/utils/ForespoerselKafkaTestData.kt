package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

fun sendNyforespoerselTilKafka() {
    val forespoerselId = UUID.randomUUID().toString()
    val oppdatertForespoerselId = UUID.randomUUID().toString()

    val forespoerselMottattJson = buildForespoerselMottattJson(forespoerselId = forespoerselId)
    println(forespoerselMottattJson)
    val forespoerselOppdaterJson =
        buildForespoerselOppdatertJson(forespoerselId = oppdatertForespoerselId, eksponertForespoerselıd = forespoerselId)
    println(forespoerselOppdaterJson)
    val priTopic = Env.getProperty("kafkaConsumer.forespoersel.topic")
    val priRecord = ProducerRecord(priTopic, "key", forespoerselMottattJson)
    Producer.sendMelding(priRecord)
    val priRecordOppdatert = ProducerRecord(priTopic, "key", forespoerselOppdaterJson)
    Producer.sendMelding(priRecordOppdatert)
    Producer.kafkaProducer.close()
}

fun main() {
    sendNyforespoerselTilKafka()
}
