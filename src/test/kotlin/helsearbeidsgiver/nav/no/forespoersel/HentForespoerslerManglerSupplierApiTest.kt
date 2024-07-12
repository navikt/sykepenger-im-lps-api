package helsearbeidsgiver.nav.no.forespoersel

import helsearbeidsgiver.nav.no.utils.FunSpecWithUnauthorizedApi
import io.kotest.matchers.shouldBe

class HentForespoerslerManglerSupplierApiTest :
    FunSpecWithUnauthorizedApi(
        mapOf(
            "scope" to "maskinporten",
            "consumer" to
                mapOf(
                    "authority" to "iso6523-actorid-upis",
                    "ID" to "0192:910753614",
                ),
        ),
        { testApi ->
            test("Gir 401 når supplier mangler i token") {
                testApi {
                    val response = get("/forespoersler")
                    response.status.value shouldBe 401
                }
            }
        },
    )
