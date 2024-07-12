package helsearbeidsgiver.nav.no.forespoersel

import helsearbeidsgiver.nav.no.utils.FunSpecWithAuthorizedApi
import io.kotest.matchers.shouldBe

class HentForespoerslerApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent forespoersler fra endepunkt") {
            testApi {
                val response = get("/forespoersler")
                response.status.value shouldBe 200
            }
        }
    })
