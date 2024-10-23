package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.db.Database.getForespoerselRepo
import java.util.UUID

class ForespoerselService {
    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> = getForespoerselRepo().hentForespoerslerForOrgnr(orgnr)

    fun hentForespoersler(): List<Forespoersel> =
        listOf(
            Forespoersel(
                UUID.randomUUID().toString(),
                "orgnr",
                "fnr",
                "NY",
            ),
        )
}
