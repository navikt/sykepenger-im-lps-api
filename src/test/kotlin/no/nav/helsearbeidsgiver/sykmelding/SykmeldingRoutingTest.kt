package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.client.request.get
import no.nav.helsearbeidsgiver.sykmelding.model.SykmeldingArbeidsgiver
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmeldingArbeidsgiver

class SykmeldingRoutingTest {
    val a = ""
}
//    FunSpec({
//        val sykmeldingService = mockk<SykmeldingService>()
//
//        beforeTest {
//            mockkStatic("no.nav.helsearbeidsgiver.auth.TokenValidationUtilsKt")
//            val mockTokenValidationContext = mockk<TokenValidationContext>()
//            every { mockTokenValidationContext.getSystembrukerOrgnr() } returns "810007843"
//            every { mockTokenValidationContext.getConsumerOrgnr() } returns "810007842"
//            every {
//                runBlocking { any<RoutingContext>().tokenValidationContext() }
//            } returns mockTokenValidationContext
//        }
//
//        afterTest { unmockkAll() }
//
//        fun routingTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
//            testApplication {
//                application {
//                    install(ContentNegotiation) { json() }
//                    routing { sykmeldingV1(sykmeldingService) }
//                }
//                block()
//            }
//        }
//
//        test("GET /v1/sykmelding/{id} skal returnere OK og sykmelding") {
//
//            val sykmeldingResponse = sykmeldingMock().toSykmeldingResponse()
//            val sykmeldingArbeidsgiver = sykmeldingResponse.toSykmeldingArbeidsgiver()
//            val id = UUID.fromString(sykmeldingResponse.id)
//
//            every { sykmeldingService.hentSykmelding(id, any()) } returns sykmeldingArbeidsgiver
//
//            routingTestApplication {
//                val response = client.get("/v1/sykmelding/$id")
//
//                response.status shouldBe HttpStatusCode.OK
//                jsonMapper.decodeFromString<SykmeldingArbeidsgiver>(response.bodyAsText()) shouldBe sykmeldingArbeidsgiver
//            }
//        }
//
//        test("GET /v1/sykmelding/{id} skal returnere NotFound når sykmelding ikke finnes") {
//
//            every { sykmeldingService.hentSykmelding(any(), any()) } returns null
//
//            routingTestApplication {
//                val response = client.get("/v1/sykmelding/${UUID.randomUUID()}")
//
//                response.status shouldBe HttpStatusCode.NotFound
//                response.bodyAsText() shouldBe "Ingen sykmelding funnet"
//            }
//        }
//
//        test("GET /v1/sykmelding/{id} skal returnere BadRequest når UUID er ugyldig") {
//            routingTestApplication {
//                val response = client.get("/v1/sykmelding/noe-helt-feil-og-ugyldig")
//
//                response.status shouldBe HttpStatusCode.BadRequest
//                response.bodyAsText() shouldBe "Ugyldig sykmelding ID parameter"
//            }
//        }
//    })

fun SykmeldingDTO.toSykmeldingArbeidsgiver(): SykmeldingArbeidsgiver =
    tilSykmeldingArbeidsgiver(this.sendSykmeldingAivenKafkaMessage, mockHentPersonFraPDL(fnr))

fun SendSykmeldingAivenKafkaMessage.toSykmeldingResponse(): SykmeldingDTO =
    SykmeldingDTO(
        event.sykmeldingId,
        kafkaMetadata.fnr,
        event.arbeidsgiver!!.orgnummer,
        this,
    )
