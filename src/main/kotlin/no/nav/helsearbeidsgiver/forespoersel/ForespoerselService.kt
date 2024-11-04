package no.nav.helsearbeidsgiver.forespoersel

class ForespoerselService(
    private val forespoerselRepository: ForespoerselRepository,
) {
    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> = forespoerselRepository.hentForespoerslerForOrgnr(orgnr)
}
