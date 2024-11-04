package no.nav.helsearbeidsgiver.utils

import org.apache.kafka.clients.producer.KafkaProducer
import java.io.File
import java.util.UUID

fun readJsonFromResources(fileName: String): String {
    val resource = KafkaProducer::class.java.getResource("/$fileName")
    return File(resource.toURI()).readText(Charsets.UTF_8)
}

fun buildInnteektsmeldingJson(forespoerselId: String = UUID.randomUUID().toString()): String {
    val filePath = "im.json"
    return readJsonFromResources(filePath).replace(
        "%%%FORESPOERSELID%%%",
        forespoerselId,
    )
}

fun buildForespoerselMottattJson(forespoerselId: String = UUID.randomUUID().toString()): String {
    val filePath = "forespoersel_mottatt.json"
    return readJsonFromResources(filePath).replace(
        "%%%FORESPOERSELID%%%",
        forespoerselId,
    )
}

fun buildInntektsmeldingDistribuertJson(forespoerselId: String = UUID.randomUUID().toString()): String {
    val filePath = "inntektsmelding_distribuert.json"
    return readJsonFromResources(filePath).replace(
        "%%%FORESPOERSELID%%%",
        forespoerselId,
    )
}
