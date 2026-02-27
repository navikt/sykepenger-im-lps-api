package no.nav.helsearbeidsgiver.utils

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.RoutingCall
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.soeknad.Sykepengesoeknad
import no.nav.helsearbeidsgiver.soeknad.SykepengesoeknadForPDF
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.pipe.orDefault

val PDFGEN_URL = getPropertyOrNull("PDFGEN_SYKMELDING_URL").orDefault { throw RuntimeException("PDFGEN_SYKMELDING_URL ikke satt") }
val PDFGEN_SOEKNAD_URL = getPropertyOrNull("PDFGEN_SOEKNAD_URL").orDefault { throw RuntimeException("PDFGEN_SOEKNAD_URL ikke satt") }

object PdfgenHttpClient {
    val httpClient = createHttpClient()
}

suspend fun genererSykmeldingPdf(sykmelding: Sykmelding): ByteArray {
    val response =
        PdfgenHttpClient.httpClient.post(PDFGEN_URL) {
            contentType(ContentType.Application.Json)
            setBody(sykmelding)
        }
    if (response.status != HttpStatusCode.OK) {
        throw RuntimeException("En feil oppstod ved generering av sykmelding PDF med pdfgen: ${response.status}")
    }
    return response.readRawBytes()
}

// TODO: Kombiner til en felles utils funksjon for generer PDF
suspend fun genererSoeknadPdf(soeknad: SykepengesoeknadForPDF): ByteArray {
    val response =
        PdfgenHttpClient.httpClient.post(PDFGEN_SOEKNAD_URL) {
            contentType(ContentType.Application.Json)
            setBody(soeknad)
        }
    if (response.status != HttpStatusCode.OK) {
        throw RuntimeException("En feil oppstod ved generering av søknad PDF med pdfgen: ${response.status}")
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
