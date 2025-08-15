package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.PriMessage
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

class ForespoerselService(
    private val forespoerselRepository: ForespoerselRepository,
) {
    @Deprecated(
        message =
            "Kan slettes når vi fjerner det utfasede endepunktet GET v1/forespoersler ." +
                "Bruk filtrerForespoersler(orgnr: String, request: ForespoerselRequest) istedenfor.",
    )
    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> {
        runCatching {
            sikkerLogger().info("Henter forespørsler for bedrift: $orgnr")
            forespoerselRepository.hentForespoersler(orgnr = orgnr)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} forespørsler for bedrift: $orgnr")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av forespørsler for bedrift: $orgnr", it)
        }
        throw RuntimeException("Feil ved henting av forespørsler for bedrift: $orgnr")
    }

    fun filtrerForespoersler(
        orgnr: String,
        request: ForespoerselRequest,
    ): List<Forespoersel> {
        runCatching {
            sikkerLogger().info("Henter forespørsler for bedrift: $orgnr")
            forespoerselRepository.hentForespoersler(orgnr = orgnr, request = request)
        }.onSuccess {
            sikkerLogger().info("Hentet ${it.size} forespørsler for bedrift: $orgnr")
            return it
        }.onFailure {
            sikkerLogger().warn("Feil ved henting av forespørsler for bedrift: $orgnr", it)
        }
        throw RuntimeException("Feil ved henting av forespørsler for bedrift: $orgnr")
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
            endreStatusAktivForespoersel(eksponertForespoerselId)
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
    }

    fun lagreNyForespoersel(
        forespoersel: ForespoerselDokument,
        status: Status = Status.AKTIV,
    ) {
        if (erDuplikat(forespoersel)) return
        runCatching {
            sikkerLogger().info("Lagrer forespørsel med id: ${forespoersel.forespoerselId}")
            forespoerselRepository.lagreForespoersel(
                forespoersel = forespoersel,
                status = status,
                eksponertForespoerselId = forespoersel.forespoerselId,
            )
        }.onSuccess {
            logger().info("Lagring av forespørsel med id: ${forespoersel.forespoerselId} fullført")
        }.onFailure {
            sikkerLogger().error("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
            throw RuntimeException("Feil ved lagring av forespørsel med id: ${forespoersel.forespoerselId}", it)
        }
    }

    fun hentForespoersel(navReferanseId: UUID): Forespoersel? = forespoerselRepository.hentForespoersel(navReferanseId)

    fun hentVedtaksperiodeId(navReferanseId: UUID): UUID? = forespoerselRepository.hentVedtaksperiodeId(navReferanseId)

    fun settBesvart(navReferanseId: UUID) {
        val forespoersel =
            forespoerselRepository.hentForespoersel(navReferanseId)
                ?: run {
                    sikkerLogger().info("Forespørsel med id: $navReferanseId finnes ikke, kan ikke oppdatere status til BESVART")
                    return
                }

        when (forespoersel.status) {
            Status.BESVART -> {
                logger().info("Forespørsel med id: $navReferanseId er allerede BESVART, ingen oppdatering nødvendig")
                return
            }

            Status.FORKASTET -> {
                val aktivForespoersel = forespoerselRepository.finnAktivForespoersler(navReferanseId)
                if (aktivForespoersel == null) {
                    logger().info(
                        "Førespørsel er oppdatert og allerede besvart Ingen oppdatering av status for: $navReferanseId",
                    )
                    return
                } else {
                    logger().info(
                        "Forespørsel $navReferanseId er oppdatert, oppdaterer status til BESVART for aktiv forespørsel med id: ${aktivForespoersel.navReferanseId}",
                    )
                    forespoerselRepository.oppdaterStatus(
                        aktivForespoersel.navReferanseId,
                        Status.BESVART,
                    )
                    return
                }
            }

            else -> {
                forespoerselRepository.oppdaterStatus(navReferanseId, Status.BESVART)
                logger().info("Oppdaterer status til BESVART for forespørsel med id: $navReferanseId")
            }
        }
    }

    fun settForkastet(navReferanseId: UUID) {
        forespoerselRepository.oppdaterStatus(navReferanseId, Status.FORKASTET)
        logger().info("Oppdaterer status til FORKASTET for forespørsel med id: $navReferanseId")
    }

    private fun endreStatusAktivForespoersel(eksponertForespoerselId: UUID) {
        val ef = forespoerselRepository.finnAktivForespoersler(eksponertForespoerselId)
        if (ef == null) {
            sikkerLogger().warn("Eksponert forespørsel med id: $eksponertForespoerselId finnes ikke")
        } else {
            if (ef.status == Status.AKTIV) {
                logger().info(
                    "Eksponert forespørsel med id: $eksponertForespoerselId er aktiv, oppdaterer status til forkastet.",
                )
                settForkastet(ef.navReferanseId)
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

    fun hentEksponertForespoerselId(forespoerselId: UUID): UUID =
        forespoerselRepository
            .hentEksponertForespoerselId(forespoerselId)
            ?.also {
                logger().info("Hentet eksponert forespørsel med id: $it for forespørsel med id: $forespoerselId")
            }
            ?: run {
                logger().error("Forespørsel med id: $forespoerselId finnes ikke")
                throw NoSuchElementException("Forespørsel med id: $forespoerselId finnes ikke")
            }

    fun lagreEllerOppdaterForespoersel(
        forespoersel: ForespoerselDokument,
        status: Status?,
        eksponertForespoerselId: UUID,
    ) {
        val hentet = forespoerselRepository.hentForespoersel(forespoersel.forespoerselId)

        if (hentet == null) {
            logger().info("Forespørsel med id: ${forespoersel.forespoerselId} og status $status finnes ikke, lagrer forespørsel.")
            forespoerselRepository.lagreForespoersel(
                forespoersel = forespoersel,
                status = status ?: Status.AKTIV,
                eksponertForespoerselId = eksponertForespoerselId,
            )
            return
        }

        if (status != null && hentet.status != status) {
            logger().info("Forespørsel med id: ${forespoersel.forespoerselId} finnes, oppdaterer status til $status.")
            forespoerselRepository.oppdaterStatus(forespoersel.forespoerselId, status)
        }
        val hentEksponertForespoerselId = hentEksponertForespoerselId(forespoersel.forespoerselId)
        if (hentEksponertForespoerselId != eksponertForespoerselId) {
            logger().info(
                "Forespørsel med id: ${forespoersel.forespoerselId} oppdaterer eksponertForespoerselId til: $eksponertForespoerselId.",
            )
            forespoerselRepository.oppdaterEksponertForespoerselId(
                forespoersel.forespoerselId,
                eksponertForespoerselId,
            )
            return
        }

        logger().info("Forespørsel med id: ${forespoersel.forespoerselId} finnes, ingen oppdatering nødvendig.")
    }
}
