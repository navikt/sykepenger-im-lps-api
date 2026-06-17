package no.nav.helsearbeidsgiver.csvimport

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class CsvLeser(
    private val ressurssti: String = "/excel-input/NAVIMREQUEST_Unit4.csv",
    private val forespoerselRepository: ForespoerselRepository,
) {
    private val logger = logger()

    fun reaktiveNavReferanseIder(): List<UUID> {
        val inputStream =
            CsvLeser::class.java.getResourceAsStream(ressurssti)
                ?: run {
                    logger.error("Fant ikke CSV-fil: $ressurssti")
                    return emptyList()
                }

        val aktiveIder =
            inputStream
                .bufferedReader()
                .use { reader ->
                    reader
                        .lineSequence()
                        .drop(1) // hopp over header-linje
                        .filter { it.isNotBlank() }
                        .mapNotNull { linje -> parseLinje(linje) }
                        .filter { it.requestStatus == "AKTIV" || it.requestStatus == "BESVART" }
                        .map { it.navReferanseId }
                        .distinct()
                        .toList()
                }

        logger.info("Fant ${aktiveIder.size} unike aktive NAV referanse-IDer i $ressurssti")
        aktiveIder.forEach { id ->
            logger.info("Aktiverer NAV referanse-ID: $id")
            forespoerselRepository.oppdaterStatus(id, Status.AKTIV)
        }

        return aktiveIder
    }

    private fun parseLinje(linje: String): NavImRequestRad? =
        runCatching {
            val kolonner = linje.split(",")
            require(kolonner.size >= 9) { "Forventet minst 9 kolonner, men fant ${kolonner.size}" }
            NavImRequestRad(
                company = kolonner[0].trim(),
                navReferanseId = UUID.fromString(kolonner[1].trim()),
                ssn = kolonner[2].trim(),
                unit = kolonner[3].trim(),
                incomeDate = kolonner[4].trim(),
                requestStatus = kolonner[5].trim(),
                externalCase = kolonner[6].trim(),
                firstDay = kolonner[7].trim(),
                lastDay = kolonner[8].trim(),
            )
        }.onFailure {
            logger.warn("Kunne ikke parse CSV-linje: '$linje' – ${it.message}")
        }.getOrNull()
}
