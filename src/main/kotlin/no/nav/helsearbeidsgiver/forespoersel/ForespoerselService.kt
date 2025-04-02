package no.nav.helsearbeidsgiver.forespoersel

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

class ForespoerselService(
    private val forespoerselRepository: ForespoerselRepository,
) {
    fun hentForespoerslerForOrgnr(orgnr: String): ForespoerselResponse {
        runCatching {
            sikkerLogger().info("Henter forespørsler for bedrift: $orgnr")
            forespoerselRepository.hentForespoerslerForOrgnr(orgnr)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} forespørsler for bedrift: $orgnr")
            return ForespoerselResponse(it.size, it)
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av forespørsler for bedrift: $orgnr", it)
        }
        throw RuntimeException("Feil ved henting av forespørsler for bedrift: $orgnr")
    }

    fun filtrerForespoerslerForOrgnr(
        consumerOrgnr: String,
        request: ForespoerselRequest,
    ): ForespoerselResponse {
        runCatching {
            sikkerLogger().info("Henter forespørsler for bedrift: $consumerOrgnr")
            forespoerselRepository.filtrerForespoersler(consumerOrgnr, request)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} forespørsler for bedrift: $consumerOrgnr")
            return ForespoerselResponse(it.size, it)
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av forespørsler for bedrift: $consumerOrgnr", it)
        }
        throw RuntimeException("Feil ved henting av forespørsler for bedrift: $consumerOrgnr")
    }

    fun hentForespoersel(forespoerselId: UUID): Forespoersel? = runBlocking { forespoerselRepository.hentForespoersel(forespoerselId) }
}
