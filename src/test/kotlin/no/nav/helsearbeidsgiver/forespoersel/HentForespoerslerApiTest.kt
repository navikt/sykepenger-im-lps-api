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
                forespoerselRepo.lagreForespoersel("123", orgnr1, "123", forespoerselDokument())
                forespoerselRepo.lagreForespoersel("123", orgnr2, "123", forespoerselDokument())
                val response = get("/forespoersler")
                response.status.value shouldBe 200
                val forespoerselSvar = response.body<List<Forespoersel>>()
                forespoerselSvar.size shouldBe 1
                forespoerselSvar[0].status shouldBe Status.AKTIV
                forespoerselSvar[0].orgnr shouldBe orgnr1
            }
        }
    })
