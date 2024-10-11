package no.nav.helsearbeidsgiver.inntektsmelding

import io.kotest.matchers.string.shouldContain
import io.ktor.client.statement.bodyAsText
import no.nav.helsearbeidsgiver.utils.FunSpecWithAuthorizedApi

class HentImApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent inntektsmeldinger fra endepunkt") {
            testApi {
                val response = get("/inntektsmeldinger")
                response.bodyAsText() shouldContain "Skumle bakverk A/S"
            }
        }
    })
