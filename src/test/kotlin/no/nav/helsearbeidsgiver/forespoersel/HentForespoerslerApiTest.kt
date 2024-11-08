package no.nav.helsearbeidsgiver.forespoersel

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.utils.FunSpecWithAuthorizedApi
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument

class HentForespoerslerApiTest :
    FunSpecWithAuthorizedApi({ testApi ->
        test("Hent forespoersler fra endepunkt") {
            testApi {
                val db = Database.init()
                val forespoerselRepo = ForespoerselRepository(db)
                val orgnr1 = "810007842"
                val orgnr2 = "810007843"
                val payload = forespoerselDokument(orgnr1, "123")
                forespoerselRepo.lagreForespoersel("123", payload)
                forespoerselRepo.lagreForespoersel("1234", forespoerselDokument(orgnr2, "123"))
                val response = get("/forespoersler")
                response.status.value shouldBe 200
                val forespoerselSvar = response.body<List<Forespoersel>>()
                forespoerselSvar.size shouldBe 1
                forespoerselSvar[0].status shouldBe Status.AKTIV
                forespoerselSvar[0].orgnr shouldBe orgnr1
                forespoerselSvar[0].dokument shouldBe payload
            }
        }
    })
