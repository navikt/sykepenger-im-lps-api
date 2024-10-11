package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.utils.FunSpecWithAuthorizedApi

class HentForespoerslerApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent forespoersler fra endepunkt") {
            testApi {
                val response = get("/forespoersler")
                response.status.value shouldBe 200
            }
        }
    })
