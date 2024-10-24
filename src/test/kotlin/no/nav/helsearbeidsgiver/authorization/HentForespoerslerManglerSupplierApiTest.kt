package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.utils.FunSpecWithUnauthorizedApi

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
                    val response1 = get("/forespoersler")
                    response1.status.value shouldBe 401
                    val response2 = get("/inntektsmeldinger")
                    response2.status.value shouldBe 401
                }
            }
        },
    )
