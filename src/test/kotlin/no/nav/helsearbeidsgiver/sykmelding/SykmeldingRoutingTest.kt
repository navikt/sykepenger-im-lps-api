package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.testing.testApplication
import io.ktor.util.pipeline.PipelineContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.security.token.support.core.context.TokenValidationContext
import java.util.UUID

class SykmeldingRoutingTest :
    FunSpec({

        val sykmeldingService = mockk<SykmeldingService>()

        beforeTest { testCase ->
//            println("Before: ${testCase.name.testName}")
            mockkStatic("no.nav.helsearbeidsgiver.auth.TokenValidationUtilsKt")

            val mockTokenValidationContext = mockk<TokenValidationContext>()

            every { mockTokenValidationContext.getSystembrukerOrgnr() } returns "810007843"
            every { mockTokenValidationContext.getConsumerOrgnr() } returns "810007842"
            every {
                runBlocking { any<PipelineContext<Unit, ApplicationCall>>().tokenValidationContext() }
            } returns mockTokenValidationContext
        }

//    "should return sykmelding for valid ID" {
//        val sykmeldingService = mockk<SykmeldingService>()
//        val sykmeldingId = UUID.randomUUID()
//        val expectedSykmelding = "expected sykmelding response"
//
//        every { sykmeldingService.hentSykmelding(sykmeldingId, any()) } returns expectedSykmelding
//
//        withTestApplication({
//            routing {
//                sykmeldingV1(sykmeldingService)
//            }
//        }) {
//            handleRequest(HttpMethod.Get, "/v1/sykmelding/$sykmeldingId") {
//                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
//            }.apply {
//                response.status() shouldBe HttpStatusCode.OK
//                response.content shouldBe expectedSykmelding
//            }
//        }
//    }

        test("skal returnere sykmelding") {

            val sykmeldingKafka = sykmeldingMock()
            val sykmeldingId = UUID.fromString(sykmeldingKafka.event.sykmeldingId)
            val sykmeldingResponse =
                SykmeldingResponse(
                    sykmeldingId.toString(),
                    sykmeldingKafka.kafkaMetadata.fnr,
                    sykmeldingKafka.event.arbeidsgiver!!.orgnummer,
//                    sykmeldingKafka.sykmelding,
                )

            every { sykmeldingService.hentSykmelding(sykmeldingId, any()) } returns sykmeldingResponse

            testApplication {
                routing { sykmeldingV1(sykmeldingService) }
                val response = client.get("/v1/sykmelding/$sykmeldingId")

                println("Fikk respons body:\n>>${response.bodyAsText()}<<")
//                response.bodyAsText().length shouldBeGreaterThan 0
                response.status shouldBe HttpStatusCode.OK
//                jsonMapper.decodeFromString<SykmeldingResponse>(response.bodyAsText()) shouldBe sykmeldingResponse
            }
        }

        test("skal returnere feil om UUID er feil") {
//            val sykmeldingService = mockk<SykmeldingService>()
            testApplication {
                routing { sykmeldingV1(sykmeldingService) }
                val response = client.get("/v1/sykmelding/noe-helt-feil-og-ugyldig")
                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe "Mottok ikke en gylid sykmelding ID parameter"
            }
        }

        test("skal returne 404 om sykmelding ikke eksisterer") {

            val sykmeldingId = UUID.randomUUID()
//            val sykmeldingService = mockk<SykmeldingService>()

            every { sykmeldingService.hentSykmelding(sykmeldingId, any()) } returns null

            testApplication {
                routing { sykmeldingV1(sykmeldingService) }
                val response = client.get("/v1/sykmelding/$sykmeldingId")
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Ingen sykmelding funnet"
            }
        }
    })
