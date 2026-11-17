package no.nav.helsearbeidsgiver.refusjon

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpStatusCode
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class RefusjonKlient(
    private val url: String,
    private val getAccessToken: () -> String,
    private val httpClient: HttpClient = createHttpClient(),
) {
    private val logger = logger()

    suspend fun hentRefusjonUtfallPdf(refusjonUtfallId: UUID): ByteArray? =
        try {
            httpClient
                .get("$url/refusjonsutfall/$refusjonUtfallId/pdf") {
                    bearerAuth(getAccessToken())
                }.readRawBytes()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                logger.warn("Fant ikke PDF for refusjonsutfall med refusjonUtfallId $refusjonUtfallId i hag-refusjon.")
                null
            } else {
                throw e
            }
        }
}
