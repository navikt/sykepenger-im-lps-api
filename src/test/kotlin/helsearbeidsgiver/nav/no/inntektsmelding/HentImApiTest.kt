package helsearbeidsgiver.nav.no.inntektsmelding

import helsearbeidsgiver.nav.no.utils.FunSpecWithAuthorizedApi
import io.kotest.matchers.string.shouldContain
import io.ktor.client.statement.bodyAsText

class HentImApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent inntektsmeldinger fra endepunkt") {
            testApi {
                val response = get("/inntektsmeldinger")
                response.bodyAsText() shouldContain "Skumle bakverk A/S"
            }
        }
    })
