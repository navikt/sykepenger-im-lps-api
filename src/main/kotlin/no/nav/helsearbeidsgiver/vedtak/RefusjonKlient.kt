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

interface RefusjonKlient {
    fun sendVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding)
}

class IkkeRefusjonKlient : RefusjonKlient {
    override fun sendVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {}
}

class RefusjonKlientImpl(
    private val url: String,
) : RefusjonKlient {
    private val httpClient = createHttpClient()
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
}
