package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.KafkaProducer
import java.io.File
import java.util.UUID

private const val FORESPOERSELID = "%%%FORESPOERSELID%%%"
private const val SYKMELDT_FNR = "%%%SYKMELDT%%%"
private const val ORGNUMMER = "%%%ORGNR%%%"

const val DEFAULT_FNR = "16076006028"
const val DEFAULT_ORG = "732812083"

fun buildInntektsmelding(
    forespoerselId: String = UUID.randomUUID().toString(),
    sykemeldtFnr: Fnr = Fnr(DEFAULT_FNR),
    orgNr: Orgnr = Orgnr(DEFAULT_ORG),
): Inntektsmelding = jsonMapper.decodeFromString<Inntektsmelding>(buildInntektsmeldingJson(forespoerselId, sykemeldtFnr, orgNr))

fun buildInntektsmeldingJson(
    forespoerselId: String = UUID.randomUUID().toString(),
    sykemeldtFnr: Fnr = Fnr(DEFAULT_FNR),
    orgNr: Orgnr = Orgnr(DEFAULT_ORG),
): String {
    val filePath = "im.json"
    return readJsonFromResources(filePath)
        .replace(FORESPOERSELID, forespoerselId)
        .replace(SYKMELDT_FNR, sykemeldtFnr.verdi)
        .replace(ORGNUMMER, orgNr.verdi)
}

fun buildForespoerselMottattJson(forespoerselId: String = UUID.randomUUID().toString()): String {
    val filePath = "forespoersel.json"
    return readJsonFromResources(filePath).replace(
        FORESPOERSELID,
        forespoerselId,
    )
}

fun buildInntektsmeldingDistribuertJson(forespoerselId: String = UUID.randomUUID().toString()): String {
    val filePath = "inntektsmelding_distribuert.json"
    return readJsonFromResources(filePath).replace(
        FORESPOERSELID,
        forespoerselId,
    )
}

fun readJsonFromResources(fileName: String): String {
    val resource = KafkaProducer::class.java.getResource("/$fileName")
    return File(resource!!.toURI()).readText(Charsets.UTF_8)
}
