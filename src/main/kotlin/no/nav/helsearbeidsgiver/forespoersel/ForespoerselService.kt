package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

class ForespoerselService(
    private val forespoerselRepository: ForespoerselRepository,
) {
    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> {
        runCatching {
            sikkerLogger().info("Henter forespørsler for bedrift: $orgnr")
            forespoerselRepository.hentForespoerslerForOrgnr(orgnr)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} forespørsler for bedrift: $orgnr")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av forespørsler for bedrift: $orgnr", it)
        }
        throw RuntimeException("Feil ved henting av forespørsler for bedrift: $orgnr")
    }

    fun filtrerForespoerslerForOrgnr(
        consumerOrgnr: String,
        request: ForespoerselRequest,
    ): List<Forespoersel> {
        runCatching {
            sikkerLogger().info("Henter forespørsler for bedrift: $consumerOrgnr")
            forespoerselRepository.filtrerForespoersler(consumerOrgnr, request)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} forespørsler for bedrift: $consumerOrgnr")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av forespørsler for bedrift: $consumerOrgnr", it)
        }
        throw RuntimeException("Feil ved henting av forespørsler for bedrift: $consumerOrgnr")
    }

    fun hentForespoersel(
        navReferanseId: UUID,
        orgnr: String,
    ): Forespoersel? =
        forespoerselRepository.hentForespoersel(
            navReferanseId,
            orgnr,
        )

    fun hentVedtaksperiodeId(navReferanseId: UUID): UUID? = forespoerselRepository.hentVedtaksperiodeId(navReferanseId)
}
