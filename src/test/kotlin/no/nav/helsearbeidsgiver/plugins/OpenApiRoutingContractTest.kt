package no.nav.helsearbeidsgiver.plugins

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.helsearbeidsgiver.authorization.ApiTest
import org.junit.jupiter.api.Test

class OpenApiRoutingContractTest : ApiTest() {
    @Test
    fun `openapi inneholder kun nødvendige routes`() {
        runBlocking {
            val response = client.get("/swagger/openapi.json")

            response.status shouldBe HttpStatusCode.OK

            val openApi = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val paths = openApi.getValue("paths").jsonObject

            fun assertPathOperation(
                path: String,
                operation: String,
            ) {
                withClue("Forventet $operation $path i openapi.json") {
                    paths.containsKey(path) shouldBe true
                    paths.getValue(path).jsonObject.containsKey(operation) shouldBe true
                }
            }

            assertPathOperation("/v1/inntekt", "get")
            assertPathOperation("/v1/inntektsmelding", "post")
            assertPathOperation("/v1/forespoersel/{navReferanseId}", "get")
            assertPathOperation("/v1/forespoersler", "post")
            assertPathOperation("/v1/sykepengesoeknad/{soeknadId}", "get")
            assertPathOperation("/v1/sykmelding/{sykmeldingId}", "get")
            assertPathOperation("/v1/sykmelding/{sykmeldingId}/pdf", "get")
            assertPathOperation("/v1/sykmeldinger", "post")

            withClue("Kun dokumenterte sykmelding-routes skal være med i openapi.json") {
                paths.containsKey("/v1/sykmelding/{sykmeldingId}.pdf") shouldBe false
            }

            withClue("Ukommenterte tokenx-routes skal ikke med i openapi.json") {
                paths.keys.any { it.startsWith("/intern/personbruker/sykmelding/") } shouldBe false
                paths.keys.any { it.startsWith("/intern/personbruker/sykepengesoeknad/") } shouldBe false
            }

            withClue("Health-routes skal ikke med i openapi.json") {
                paths.containsKey("/health/is-alive") shouldBe false
                paths.containsKey("/health/is-ready") shouldBe false
            }

            withClue("mottattAvNav skal dokumenteres som date-time") {
                val mottattAvNavSchema =
                    openApi
                        .getValue("components")
                        .jsonObject
                        .getValue("schemas")
                        .jsonObject
                        .getValue("Sykmelding")
                        .jsonObject
                        .getValue("properties")
                        .jsonObject
                        .getValue("mottattAvNav")
                        .jsonObject

                mottattAvNavSchema.getValue("type").jsonPrimitive.content shouldBe "string"
                mottattAvNavSchema.getValue("format").jsonPrimitive.content shouldBe "date-time"
            }
        }
    }
}
