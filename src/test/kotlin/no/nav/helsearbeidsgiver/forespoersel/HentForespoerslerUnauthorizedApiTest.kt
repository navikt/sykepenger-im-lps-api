package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.utils.FunSpecWithUnauthorizedApi

class HentForespoerslerUnauthorizedApiTest :
    FunSpecWithUnauthorizedApi(emptyMap(), { testApi ->
        test("Gir 401 når claim mangler i token") {
            testApi {
                val response = get("/forespoersler")
                response.status.value shouldBe 401
            }
        }
    })
