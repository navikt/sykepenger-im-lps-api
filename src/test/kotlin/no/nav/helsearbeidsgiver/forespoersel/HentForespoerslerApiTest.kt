package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import no.nav.helsearbeidsgiver.utils.FunSpecWithAuthorizedApi

class HentForespoerslerApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent forespoersler fra endepunkt") {
            testApi {
                val response = get("/forespoersler")
                response.status.value shouldBe 200
                val forespoerselSvar = response.body<List<Forespoersel>>()
                forespoerselSvar.size shouldBe 1
                forespoerselSvar[0].status shouldBe "NY"
                forespoerselSvar[0].orgnr shouldBe "810007842"
            }
        }
    })
