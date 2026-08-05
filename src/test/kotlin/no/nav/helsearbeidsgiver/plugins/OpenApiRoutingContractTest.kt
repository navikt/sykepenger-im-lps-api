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
            withClue("OpenAPI YAML kunne ikke parses") {
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
                    "/v1/sykmelding/{sykmeldingId}" to PathItem.HttpMethod.GET,
                    "/v1/sykmelding/{sykmeldingId}/pdf" to PathItem.HttpMethod.GET,
                    "/v1/sykmeldinger" to PathItem.HttpMethod.POST,
                )

            expectedOperations.forEach { (path, method) ->
                withClue("Forventet $method $path i documentation.yaml") {
                    (operation(path, method) != null) shouldBe true
                }
            }

            withClue("GET /v1/forespoersel/{navReferanseId} skal dokumentere forventede responser inkl. 403") {
                val getForespoersel = operation("/v1/forespoersel/{navReferanseId}", PathItem.HttpMethod.GET)
                (getForespoersel != null) shouldBe true
                listOf("200", "400", "401", "403", "404", "500").forEach { statusCode ->
                    (getForespoersel?.responses?.containsKey(statusCode) == true) shouldBe true
                }
            }

            withClue("POST /v1/forespoersler skal dokumentere forventede responser inkl. 403 og warning-header") {
                val postForespoersler = operation("/v1/forespoersler", PathItem.HttpMethod.POST)
                (postForespoersler != null) shouldBe true
                listOf("200", "400", "401", "403", "500").forEach { statusCode ->
                    (postForespoersler?.responses?.containsKey(statusCode) == true) shouldBe true
                }
                (
                    postForespoersler
                        ?.responses
                        ?.get("200")
                        ?.headers
                        ?.containsKey("X-Warning-limit-reached") == true
                ) shouldBe true
            }

            withClue("sykmeldingId path-parameter skal dokumenteres med uuid-format") {
                fun assertSykmeldingIdUuidFormat(op: Operation?) {
                    val sykmeldingIdParameter =
                        op
                            ?.parameters
                            ?.firstOrNull { parameter ->
                                parameter.name == "sykmeldingId" && parameter.`in` == "path"
                            }
                    (sykmeldingIdParameter != null) shouldBe true
                    val schema = sykmeldingIdParameter?.schema
                    isStringSchema(schema) shouldBe true
                    (schema?.format == "uuid") shouldBe true
                }
                assertSykmeldingIdUuidFormat(operation("/v1/sykmelding/{sykmeldingId}", PathItem.HttpMethod.GET))
                assertSykmeldingIdUuidFormat(operation("/v1/sykmelding/{sykmeldingId}/pdf", PathItem.HttpMethod.GET))
            }

            withClue("Kun dokumenterte sykmelding-routes skal være med i documentation.yaml") {
                (openApi.paths?.containsKey("/v1/sykmelding/{sykmeldingId}.pdf") == true) shouldBe false
            }

            withClue("Ukommenterte tokenx-routes skal ikke med i documentation.yaml") {
                openApi.paths?.keys?.any { it.startsWith("/intern/personbruker/sykmelding/") } shouldBe false
                openApi.paths?.keys?.any { it.startsWith("/intern/personbruker/sykepengesoeknad/") } shouldBe false
            }

            withClue("Health-routes skal ikke med i documentation.yaml") {
                (openApi.paths?.containsKey("/health/is-alive") == true) shouldBe false
                (openApi.paths?.containsKey("/health/is-ready") == true) shouldBe false
            }

            withClue("Global security og bearerAuth securityScheme skal være gyldig definert") {
                (openApi.security?.isNotEmpty() == true) shouldBe true
                (openApi.security?.firstOrNull()?.containsKey("bearerAuth") == true) shouldBe true
                (
                    openApi.security
                        ?.firstOrNull()
                        ?.get("bearerAuth")
                        ?.isEmpty() == true
                ) shouldBe true

                val bearerAuthScheme = openApi.components?.securitySchemes?.get("bearerAuth")
                (bearerAuthScheme != null) shouldBe true
                (bearerAuthScheme?.type == io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP) shouldBe true
                (bearerAuthScheme?.scheme == "bearer") shouldBe true
            }
        }
    }
}
