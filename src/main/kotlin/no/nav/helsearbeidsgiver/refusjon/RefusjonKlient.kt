package no.nav.helsearbeidsgiver.refusjon

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

interface RefusjonKlient {
    fun sendVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding)

    /** @return PDF-en for refusjonsutfallet, eller null dersom hag-refusjon ikke finner den. */
    suspend fun hentRefusjonUtfallPdf(refusjonUtfallId: UUID): ByteArray?
}

class IkkeRefusjonKlient : RefusjonKlient {
    override fun sendVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {}

    override suspend fun hentRefusjonUtfallPdf(refusjonUtfallId: UUID): ByteArray? =
        throw UnsupportedOperationException("Henting av PDF fra hag-refusjon er kun tilgjengelig i dev.")
}

class RefusjonKlientImpl(
    private val url: String,
    private val httpClient: HttpClient = createHttpClient(),
) : RefusjonKlient {
    private val logger = logger()
    private val sikkerLogger = sikkerLogger()

    override fun sendVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        val vedtaksperiodeId = vedtakArbeidsgiverMelding.vedtaksperiodeId
        try {
            runBlocking {
                httpClient.post("$url/arbeidstaker-vedtak") {
                    contentType(ContentType.Application.Json)
                    setBody(vedtakArbeidsgiverMelding)
                }
            }
            logger.info("Sendte vedtak med vedtaksperiodeId $vedtaksperiodeId til refusjon.")
        } catch (e: Exception) {
            val feilmelding = "Klarte ikke å sende vedtak med vedtaksperiodeId $vedtaksperiodeId til refusjon."
            logger.warn(feilmelding)
            sikkerLogger.warn(feilmelding, e)
        }
    }

    override suspend fun hentRefusjonUtfallPdf(refusjonUtfallId: UUID): ByteArray? =
        try {
            httpClient.get("$url/refusjonsutfall/$refusjonUtfallId/pdf").readRawBytes()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                logger.warn("Fant ikke PDF for refusjonsutfall med refusjonUtfallId $refusjonUtfallId i hag-refusjon.")
                null
            } else {
                throw e
            }
        }
}
