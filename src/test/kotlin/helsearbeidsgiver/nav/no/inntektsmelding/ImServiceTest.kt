package helsearbeidsgiver.nav.no.inntektsmelding

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize

class ImServiceTest :
    FunSpec({
        test("ImService returnerer liste med inntektsmeldinger") {
            val service = ImService()
            service.hentInntektsmeldinger() shouldHaveSize 1
        }
    })
