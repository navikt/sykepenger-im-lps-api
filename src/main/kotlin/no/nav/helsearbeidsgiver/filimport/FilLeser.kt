package no.nav.helsearbeidsgiver.filimport

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class FilLeser(
    private val ressurssti: String = "/excel-input/fsp_tilbakestill.txt",
    private val forespoerselRepository: ForespoerselRepository,
) {
    private val logger = logger()

    fun tilbakestillForespoerslerTilStatusAktiv(): List<UUID> {
        val inputStream =
            FilLeser::class.java.getResourceAsStream(ressurssti)
                ?: run {
                    logger.error("Fant ikke fil: $ressurssti")
                    return emptyList()
                }

        val aktiveIder =
            inputStream
                .bufferedReader()
                .use { reader ->
                    reader
                        .lines()
                        .filter { it.isNotBlank() }
                        .map {
                            parseUUID(it.trim())
                        }.toList()
                        .filterNotNull()
                        .distinct()
                }

        logger.info("Fant ${aktiveIder.size} unike aktive NAV referanse-IDer i $ressurssti")
        aktiveIder.forEach { id ->
            logger.info("Aktiverer NAV referanse-ID: $id")
            forespoerselRepository.oppdaterStatus(id, Status.AKTIV)
        }
        return aktiveIder
    }

    private fun parseUUID(string: String): UUID? {
        try {
            return UUID.fromString(string)
        } catch (e: IllegalArgumentException) {
            logger.warn("Feil ved parsing av UUID: $string", e)
            return null
        }
    }
}
