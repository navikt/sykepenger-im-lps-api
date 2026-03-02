package no.nav.helsearbeidsgiver.utils

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.RoutingCall
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.soeknad.SykepengesoeknadForPDF
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.pipe.orDefault

val PDFGEN_URL = getPropertyOrNull("PDFGEN_SYKMELDING_URL").orDefault { throw RuntimeException("PDFGEN_SYKMELDING_URL ikke satt") }
val PDFGEN_SOEKNAD_URL = getPropertyOrNull("PDFGEN_SOEKNAD_URL").orDefault { throw RuntimeException("PDFGEN_SOEKNAD_URL ikke satt") }

object PdfgenHttpClient {
    val httpClient = createHttpClient()
}

suspend fun genererSykmeldingPdf(sykmelding: Sykmelding) = pdfgenKall(sykmelding, PDFGEN_URL)

suspend fun genererSoeknadPdf(soeknad: SykepengesoeknadForPDF) = pdfgenKall(soeknad, PDFGEN_SOEKNAD_URL)

private suspend fun pdfgenKall(
    body: Any?,
    url: String,
): ByteArray {
    val response =
        PdfgenHttpClient.httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    if (response.status != HttpStatusCode.OK) {
        "En feil oppstod ved generering av PDF med pdfgen til $url: ${response.status}\nBody: ${response.bodyAsText()}"
            .also { throw RuntimeException(it) }
    }
    return response.readRawBytes()
}

suspend fun RoutingCall.respondMedPDF(
    bytes: ByteArray,
    filnavn: String,
) {
    this.response.header(HttpHeaders.ContentDisposition, "inline; filename=\"$filnavn\"")
    this.respondBytes(
        bytes = bytes,
        contentType = ContentType.Application.Pdf,
        status = HttpStatusCode.OK,
    )
}
