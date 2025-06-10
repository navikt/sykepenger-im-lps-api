package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.utils.log.logger
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

    fun lagreOppdatertForespoersel(priMessage: PriMessage) {
        val forespoersel =
            priMessage.forespoersel
                ?: throw IllegalArgumentException("Forespørsel må ikke være null i oppdatert forespørsel")
        val eksponertForespoerselId = priMessage.eksponertForespoerselId
        if (eksponertForespoerselId == null) {
            logger().info("Forespørsel : ${forespoersel.forespoerselId}: Eksponert forespørsel ID er null, behandler som ny forespørsel")
            lagreForespoersel(forespoersel = forespoersel)
            return
        } else {
            val ef = forespoerselRepository.hentForespoersel(eksponertForespoerselId)
            if (ef == null) {
                logger().info(
                    "Forespørsel : ${forespoersel.forespoerselId} :Eksponert forespørsel med id: $eksponertForespoerselId finnes ikke, behandler som ny forespørsel",
                )
                lagreForespoersel(forespoersel = forespoersel)
                return
            } else {
                runCatching {
                    sikkerLogger().info("Lagrer oppdatert forespørsel med id: ${forespoersel.forespoerselId}")
                    forespoerselRepository.lagreForespoersel(
                        forespoersel = forespoersel,
                        status = Status.AKTIV,
                        eksponertForespoerselId = eksponertForespoerselId,
                    )
                    if (ef.status == Status.AKTIV) {
                        forespoerselRepository.settForkastet(eksponertForespoerselId)
                    }
                }.onSuccess {
                    sikkerLogger().info("Lagring av oppdatert forespørsel med id: ${forespoersel.forespoerselId} fullført")
                }.onFailure {
                    sikkerLogger().error("Feil ved lagring av oppdatert forespørsel med id: ${forespoersel.forespoerselId}", it)
                    throw RuntimeException("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
                }
            }
        }
    }

    fun lagreForespoersel(
        forespoersel: ForespoerselDokument,
        status: Status = Status.AKTIV,
    ) {
        val f = forespoerselRepository.hentForespoersel(forespoersel.forespoerselId)
        if (f != null) {
            sikkerLogger().warn("Duplikat id: ${forespoersel.forespoerselId}, kan ikke lagre")
            return
        }
        runCatching {
            sikkerLogger().info("Lagrer forespørsel med id: ${forespoersel.forespoerselId}")
            forespoerselRepository.lagreForespoersel(
                forespoersel = forespoersel,
                status = status,
                eksponertForespoerselId = null,
            )
        }.onSuccess {
            sikkerLogger().info("Lagring av forespørsel med id: ${forespoersel.forespoerselId} fullført")
        }.onFailure {
            sikkerLogger().error("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
            throw RuntimeException("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
        }
    }

    fun hentForespoersel(navReferanseId: UUID): Forespoersel? = forespoerselRepository.hentForespoersel(navReferanseId)

    fun hentVedtaksperiodeId(navReferanseId: UUID): UUID? = forespoerselRepository.hentVedtaksperiodeId(navReferanseId)
}
