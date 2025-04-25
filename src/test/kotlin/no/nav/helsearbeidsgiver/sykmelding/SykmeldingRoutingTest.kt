package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.auth.getConsumerOrgnr
import no.nav.helsearbeidsgiver.auth.getSystembrukerOrgnr
import no.nav.helsearbeidsgiver.auth.tokenValidationContext
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmelding
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
                runBlocking { any<RoutingContext>().tokenValidationContext() }
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

        test("GET /v1/sykmelding/{id} skal returnere OK og sykmelding") {

            val sykmeldingDTO = sykmeldingMock().tilSykmeldingDTO()
            val sykmelding = sykmeldingDTO.tilMockSykmeldingModel()
            val id = UUID.fromString(sykmeldingDTO.id)

            every { sykmeldingService.hentSykmelding(id, any()) } returns sykmelding

            routingTestApplication {
                val response = client.get("/v1/sykmelding/$id")

                response.status shouldBe HttpStatusCode.OK
                jsonMapper.decodeFromString<Sykmelding>(response.bodyAsText()) shouldBe sykmelding
            }
        }

        test("GET /v1/sykmelding/{id} skal returnere NotFound når sykmelding ikke finnes") {

            every { sykmeldingService.hentSykmelding(any(), any()) } returns null

            routingTestApplication {
                val response = client.get("/v1/sykmelding/${UUID.randomUUID()}")

                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Ingen sykmelding funnet"
            }
        }

        test("GET /v1/sykmelding/{id} skal returnere BadRequest når UUID er ugyldig") {
            routingTestApplication {
                val response = client.get("/v1/sykmelding/noe-helt-feil-og-ugyldig")

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe "Ugyldig sykmelding ID parameter"
            }
        }
    })

fun SykmeldingDTO.tilMockSykmeldingModel(): Sykmelding = tilSykmelding()

fun SendSykmeldingAivenKafkaMessage.tilSykmeldingDTO(): SykmeldingDTO =
    SykmeldingDTO(
        event.sykmeldingId,
        kafkaMetadata.fnr,
        event.arbeidsgiver.orgnummer,
        this,
        "Ola Nordmann",
    )
