package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.utils.FunSpecWithUnauthorizedApi

class HentForespoerslerUnauthorizedApiTest :
    FunSpecWithUnauthorizedApi(emptyMap(), { testApi ->
        test("Gir 401 når claim mangler i token") {
            testApi {
                val response1 = get("/forespoersler")
                response1.status.value shouldBe 401
                val response2 = get("/inntektsmeldinger")
                response2.status.value shouldBe 401
            }
        }
    })
