package helsearbeidsgiver.nav.no.utils

import helsearbeidsgiver.nav.no.module
import io.kotest.core.spec.style.FunSpec
import io.ktor.server.testing.testApplication
import no.nav.security.mock.oauth2.MockOAuth2Server

abstract class FunSpecWithAuthorizedApi(
    block: FunSpec.((block: suspend TestClient.() -> Unit) -> Unit) -> Unit,
) : FunSpecWithApi(
        claims =
            mapOf(
                "scope" to "maskinporten",
                "supplier" to
                    mapOf(
                        "authority" to "iso6523-actorid-upis",
                        "ID" to "0192:991825827",
                    ),
                "consumer" to
                    mapOf(
                        "authority" to "iso6523-actorid-upis",
                        "ID" to "0192:910753614",
                    ),
            ),
        block = block,
    )

abstract class FunSpecWithUnauthorizedApi(
    claims: Map<String, Any>,
    block: FunSpec.((block: suspend TestClient.() -> Unit) -> Unit) -> Unit,
) : FunSpecWithApi(
        claims = claims,
        block = block,
    )

abstract class FunSpecWithApi(
    claims: Map<String, Any>,
    block: FunSpec.((block: suspend TestClient.() -> Unit) -> Unit) -> Unit,
) : FunSpec({
        val mockOAuth2Server = MockOAuth2Server()
        val port = 33445

        fun mockAuthToken(): String =
            mockOAuth2Server
                .issueToken(
                    issuerId = "maskinporten",
                    audience = "nav:inntektsmelding/lps.write",
                    claims = claims,
                ).serialize()

        fun testApi(block: suspend TestClient.() -> Unit): Unit =
            testApplication {
                val testClient = TestClient(this, ::mockAuthToken)
                testClient.block()
            }

        beforeEach {
            mockOAuth2Server.start(port)
        }
        afterEach {
            mockOAuth2Server.shutdown()
        }
        block { testApi(it) }
    }) {
    val mockOAuth2Server = MockOAuth2Server()
}

