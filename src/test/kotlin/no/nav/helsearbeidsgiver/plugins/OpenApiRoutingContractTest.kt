package no.nav.helsearbeidsgiver.plugins

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.core.models.ParseOptions
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.authorization.ApiTest
import org.junit.jupiter.api.Test

class OpenApiRoutingContractTest : ApiTest() {
    @Test
    fun `openapi inneholder kun nødvendige routes`() {
        runBlocking {
            val response = client.get("/swagger/documentation.yaml")

            response.status shouldBe HttpStatusCode.OK
            val yaml = response.bodyAsText()

            val parseResult =
                OpenAPIParser().readContents(
                    yaml,
                    null,
                    ParseOptions().apply { isResolve = true },
                )
            val openApi = parseResult.openAPI
            withClue("OpenAPI YAML kunne ikke parses.") {
                (openApi != null) shouldBe true
            }
            openApi ?: return@runBlocking

            fun operation(
                path: String,
                method: PathItem.HttpMethod,
            ): Operation? =
                openApi.paths
                    ?.get(path)
                    ?.readOperationsMap()
                    ?.get(method)

            fun isStringSchema(schema: Schema<*>?): Boolean = schema?.type == "string" || (schema?.types?.contains("string") == true)

            val expectedOperations =
                listOf(
                    "/v1/inntekt" to PathItem.HttpMethod.GET,
                    "/v1/inntektsmelding" to PathItem.HttpMethod.POST,
                    "/v1/inntektsmelding/{innsendingId}" to PathItem.HttpMethod.GET,
                    "/v1/inntektsmeldinger" to PathItem.HttpMethod.POST,
                    "/v1/forespoersel/{navReferanseId}" to PathItem.HttpMethod.GET,
                    "/v1/forespoersler" to PathItem.HttpMethod.POST,
                    "/v1/sykepengesoeknad/{soeknadId}" to PathItem.HttpMethod.GET,
                    "/v1/sykepengesoeknad/{soeknadId}/pdf" to PathItem.HttpMethod.GET,
                    "/v1/sykepengesoeknader" to PathItem.HttpMethod.POST,
                    "/v1/sykmelding/{sykmeldingId}" to PathItem.HttpMethod.GET,
                    "/v1/sykmelding/{sykmeldingId}/pdf" to PathItem.HttpMethod.GET,
                    "/v1/sykmeldinger" to PathItem.HttpMethod.POST,
                )

            expectedOperations.forEach { (path, method) ->
                withClue("Forventet $method $path i documentation.yaml") {
                    (operation(path, method) != null) shouldBe true
                }
            }

            withClue("Alle dokumenterte /v1-operasjoner skal være eksplisitt dekket i testen") {
                val actualV1Operations =
                    openApi.paths
                        .orEmpty()
                        .flatMap { (path, pathItem) ->
                            pathItem.readOperationsMap().keys.map { method -> path to method }
                        }.toSet()
                actualV1Operations shouldBe expectedOperations.toSet()
            }
        }
    }
}
