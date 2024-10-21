package no.nav.helsearbeidsgiver.forespoersel

import java.util.UUID

class ForespoerselService {
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
