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

        val eksponertForespoerselId =
            priMessage.eksponertForespoerselId
                ?: forespoersel.forespoerselId
        runCatching {
            if (erDuplikat(forespoersel)) return
            logger().info(
                "Lagrer oppdatert forespørsel med id: ${forespoersel.forespoerselId} og eksponertForespoerselId: $eksponertForespoerselId",
            )
            forespoerselRepository.lagreForespoersel(
                forespoersel = forespoersel,
                status = Status.AKTIV,
                eksponertForespoerselId = eksponertForespoerselId,
            )
        }.onSuccess {
            logger().info("Lagring av oppdatert forespørsel med id: ${forespoersel.forespoerselId} fullført")
        }.onFailure {
            sikkerLogger().error("Feil ved lagring av oppdatert forespørsel med id: ${forespoersel.forespoerselId}", it)
            throw RuntimeException("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
        }
        if (priMessage.eksponertForespoerselId != null) {
            logger().info("Endrer status for eksponert forespørsel med id: $eksponertForespoerselId")
            endreStatusAktivForespoersel(priMessage.eksponertForespoerselId, forespoersel)
        }
    }

    fun lagreNyForespoersel(forespoersel: ForespoerselDokument) {
        if (erDuplikat(forespoersel)) return
        runCatching {
            sikkerLogger().info("Lagrer forespørsel med id: ${forespoersel.forespoerselId}")
            forespoerselRepository.lagreForespoersel(
                forespoersel = forespoersel,
                status = Status.AKTIV,
                eksponertForespoerselId = forespoersel.forespoerselId,
            )
        }.onSuccess {
            logger().info("Lagring av forespørsel med id: ${forespoersel.forespoerselId} fullført")
        }.onFailure {
            sikkerLogger().error("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
            throw RuntimeException("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
        }
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

    fun settBesvart(navReferanseId: UUID) {
        forespoerselRepository.oppdaterStatus(navReferanseId, Status.BESVART)
        logger().info("Oppdaterer status til BESVART for forespørsel med id: $navReferanseId")
    }

    fun settForkastet(navReferanseId: UUID) {
        forespoerselRepository.oppdaterStatus(navReferanseId, Status.FORKASTET)
        logger().info("Oppdaterer status til FORKASTET for forespørsel med id: $navReferanseId")
    }

    private fun endreStatusAktivForespoersel(
        eksponertForespoerselId: UUID,
        forespoersel: ForespoerselDokument,
    ) {
        val ef = forespoerselRepository.hentForespoersel(eksponertForespoerselId, forespoersel.orgnr)
        if (ef == null) {
            sikkerLogger().warn("Eksponert forespørsel med id: $eksponertForespoerselId finnes ikke")
        } else {
            if (ef.status == Status.AKTIV) {
                logger().info(
                    "Eksponert forespørsel med id: $eksponertForespoerselId er aktiv, oppdaterer status til forkastet.",
                )
                settForkastet(eksponertForespoerselId)
            } else {
                logger().info(
                    "Eksponert forespørsel med id: $eksponertForespoerselId er ikke aktiv, ingen oppdatering av status.",
                )
            }
        }
    }

    private fun erDuplikat(forespoersel: ForespoerselDokument): Boolean {
        val f = forespoerselRepository.hentForespoersel(forespoersel.forespoerselId, forespoersel.orgnr)
        if (f != null) {
            logger().warn("Duplikat id: ${forespoersel.forespoerselId}, kan ikke lagre")
            return true
        }
        return false
    }
}
