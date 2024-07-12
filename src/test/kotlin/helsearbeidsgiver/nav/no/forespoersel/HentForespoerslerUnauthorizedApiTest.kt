package helsearbeidsgiver.nav.no.forespoersel

import helsearbeidsgiver.nav.no.utils.FunSpecWithUnauthorizedApi
import io.kotest.matchers.shouldBe

class HentForespoerslerUnauthorizedApiTest :
    FunSpecWithUnauthorizedApi(emptyMap(), { testApi ->
        test("Gir 401 når claim mangler i token") {
            testApi {
                val response = get("/forespoersler")
                response.status.value shouldBe 401
            }
        }
    })
