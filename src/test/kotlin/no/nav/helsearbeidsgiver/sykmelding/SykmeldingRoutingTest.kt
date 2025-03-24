package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.util.pipeline.PipelineContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.security.token.support.core.context.TokenValidationContext
import java.util.UUID

class SykmeldingRoutingTest :
    FunSpec({
        val sykmeldingService = mockk<SykmeldingService>()

        beforeTest {
            mockkStatic("no.nav.helsearbeidsgiver.auth.TokenValidationUtilsKt")
            val mockTokenValidationContext = mockk<TokenValidationContext>()
            every { mockTokenValidationContext.getSystembrukerOrgnr() } returns "810007843"
            every { mockTokenValidationContext.getConsumerOrgnr() } returns "810007842"
            every {
                runBlocking { any<PipelineContext<Unit, ApplicationCall>>().tokenValidationContext() }
            } returns mockTokenValidationContext
        }

        afterTest { unmockkAll() }

        fun routingTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing { sykmeldingV1(sykmeldingService) }
                }
                block()
            }
        }

        test("skal returnere sykmelding") {

            val sykmeldingKafka = sykmeldingMock()
            val sykmeldingId = UUID.fromString(sykmeldingKafka.event.sykmeldingId)
            val sykmeldingResponse =
                SykmeldingResponse(
                    sykmeldingId.toString(),
                    sykmeldingKafka.kafkaMetadata.fnr,
                    sykmeldingKafka.event.arbeidsgiver!!.orgnummer,
                    sykmeldingKafka.sykmelding,
                )

            every { sykmeldingService.hentSykmelding(sykmeldingId, any()) } returns sykmeldingResponse

            routingTestApplication {
                val response = client.get("/v1/sykmelding/$sykmeldingId")

                println("Fikk respons body:\n>>${response.bodyAsText()}<<")

                response.status shouldBe HttpStatusCode.OK
                jsonMapper.decodeFromString<SykmeldingResponse>(response.bodyAsText()) shouldBe sykmeldingResponse
            }
        }

        test("skal returnere feil om sykmelding ikke funnet") {

            every { sykmeldingService.hentSykmelding(any(), any()) } returns null

            routingTestApplication {
                val response = client.get("/v1/sykmelding/${UUID.randomUUID()}")

                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Ingen sykmelding funnet"
            }
        }

        test("skal returnere feil om UUID er feil") {
            routingTestApplication {
                val response = client.get("/v1/sykmelding/noe-helt-feil-og-ugyldig")
                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe "Ugyldig sykmelding ID parameter"
            }
        }
    })
