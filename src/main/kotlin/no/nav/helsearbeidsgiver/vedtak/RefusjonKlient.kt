package no.nav.helsearbeidsgiver.vedtak

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class RefusjonKlient(
    private val url: String?,
) {
    private val httpClient = createHttpClient()
    private val logger = logger()
    private val sikkerLogger = sikkerLogger()

    fun sendVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        if (url == null) return

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
}
