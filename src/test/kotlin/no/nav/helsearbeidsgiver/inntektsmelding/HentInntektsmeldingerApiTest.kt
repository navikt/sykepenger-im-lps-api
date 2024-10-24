package no.nav.helsearbeidsgiver.inntektsmelding

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import no.nav.helsearbeidsgiver.utils.FunSpecWithAuthorizedApi
import no.nav.helsearbeidsgiver.utils.json.toJson

class HentInntektsmeldingerApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent inntektsmeldinger fra endepunkt") {
            testApi {
                val response = get("/inntektsmeldinger")
                response.status.value shouldBe 200
                val inntektsmeldinger = response.body<List<ExposedInntektsmelding>>()
                inntektsmeldinger.size shouldBe 1
                inntektsmeldinger[0].orgnr shouldBe "810007842"
                inntektsmeldinger[0].dokument["vedtaksperiodeId"] shouldBe "13129b6c-e9f5-4b1c-a855-abca47ac3d7f".toJson()
            }
        }
    })
