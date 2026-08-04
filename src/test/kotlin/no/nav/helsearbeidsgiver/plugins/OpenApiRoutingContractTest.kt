package no.nav.helsearbeidsgiver.plugins

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import org.junit.jupiter.api.Test

class OpenApiRoutingContractTest : ApiTest() {
    @Test
    fun `openapi inneholder kun nødvendige routes`() {
        runBlocking {
            val response = client.get("/swagger/documentation.yaml")

            response.status shouldBe HttpStatusCode.OK
            val openApi = response.bodyAsText()

            fun assertPathOperation(
                path: String,
                operation: String,
            ) {
                withClue("Forventet $operation $path i documentation.yaml") {
                    Regex("""(?m)^\s{2}${Regex.escape(path)}:\n\s{4}${Regex.escape(operation)}:""").containsMatchIn(openApi) shouldBe true
                }
            }

            assertPathOperation("/v1/inntekt", "get")
            assertPathOperation("/v1/inntektsmelding", "post")
            assertPathOperation("/v1/inntektsmelding/{innsendingId}", "get")
            assertPathOperation("/v1/inntektsmeldinger", "post")
            assertPathOperation("/v1/forespoersel/{navReferanseId}", "get")
            assertPathOperation("/v1/forespoersler", "post")
            assertPathOperation("/v1/sykepengesoeknad/{soeknadId}", "get")
            assertPathOperation("/v1/sykmelding/{sykmeldingId}", "get")
            assertPathOperation("/v1/sykmelding/{sykmeldingId}/pdf", "get")
            assertPathOperation("/v1/sykmeldinger", "post")

            withClue("Kun dokumenterte sykmelding-routes skal være med i documentation.yaml") {
                openApi.contains("/v1/sykmelding/{sykmeldingId}.pdf:") shouldBe false
            }

            withClue("Ukommenterte tokenx-routes skal ikke med i documentation.yaml") {
                openApi.contains("/intern/personbruker/sykmelding/") shouldBe false
                openApi.contains("/intern/personbruker/sykepengesoeknad/") shouldBe false
            }

            withClue("Health-routes skal ikke med i documentation.yaml") {
                openApi.contains("/health/is-alive") shouldBe false
                openApi.contains("/health/is-ready") shouldBe false
            }

            withClue("mottattAvNav skal dokumenteres som date-time") {
                Regex("""(?s)mottattAvNav:\s*\n\s*type:\s*"?string"?\s*\n\s*format:\s*"?date-time"?""")
                    .containsMatchIn(openApi) shouldBe true
            }

            withClue("fom og tom skal dokumenteres som date") {
                Regex("""(?s)fom:\s*\n\s*type:\s*"?string"?\s*\n\s*format:\s*"?date"?""").containsMatchIn(openApi) shouldBe true
                Regex("""(?s)tom:\s*\n\s*type:\s*"?string"?\s*\n\s*format:\s*"?date"?""").containsMatchIn(openApi) shouldBe true
            }

            withClue("SykmeldingFilter fom/tom skal dokumenteres som nullable date") {
                Regex("""(?s)SykmeldingFilter:.*?fom:\s*\n\s*type:\s*"?string"?\s*\n\s*format:\s*"?date"?""")
                    .containsMatchIn(openApi) shouldBe true
                Regex("""(?s)SykmeldingFilter:.*?tom:\s*\n\s*type:\s*"?string"?\s*\n\s*format:\s*"?date"?""")
                    .containsMatchIn(openApi) shouldBe true
            }
        }
    }
}
