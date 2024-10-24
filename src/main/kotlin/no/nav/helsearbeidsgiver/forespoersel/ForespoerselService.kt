package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.db.Database.getForespoerselRepo

class ForespoerselService {
    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> = getForespoerselRepo().hentForespoerslerForOrgnr(orgnr)
}
