package no.nav.helsearbeidsgiver.inntektsmelding

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.utils.FunSpecWithAuthorizedApi
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import java.time.LocalDateTime

class HentInntektsmeldingerApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent inntektsmeldinger fra endepunkt") {
            testApi {
                val db = Database.init()
                val forespoerselId = "13129b6c-e9f5-4b1c-a855-abca47ac3d7f"
                val im = buildInntektsmelding(forespoerselId = forespoerselId)
                InntektsmeldingRepository(db).opprett(im, "810007842", "12345678912", LocalDateTime.now(), forespoerselId)

                val response = get("/inntektsmeldinger")
                response.status.value shouldBe 200
                val inntektsmeldingResponse = response.body<InntektsmeldingResponse>()
                inntektsmeldingResponse.antallInntektsmeldinger shouldBe 1
                inntektsmeldingResponse.inntektsmeldinger[0].orgnr shouldBe "810007842"
            }
        }
    })
