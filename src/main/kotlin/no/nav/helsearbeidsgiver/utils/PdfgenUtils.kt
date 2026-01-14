package no.nav.helsearbeidsgiver.utils

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.pipe.orDefault

val PDFGEN_URL = getPropertyOrNull("PDFGEN_SYKMELDING_URL").orDefault { throw RuntimeException("PDFGEN_SYKMELDING_URL ikke satt") }

suspend fun genererSykmeldingPdf(sykmelding: Sykmelding): ByteArray {
    val httpClient = createHttpClient()
    val response =
        httpClient.post(PDFGEN_URL) {
            contentType(ContentType.Application.Json)
            setBody(sykmelding)
        }
    if (response.status != HttpStatusCode.OK) {
        throw RuntimeException("En feil oppstod ved generering av sykmelding PDF med pdfgen: ${response.status}")
    }
    return response.readRawBytes()
}
