package no.nav.helsearbeidsgiver.kafka.forespoersel.pri

import kotlinx.serialization.Serializable

@Serializable
enum class NotisType {
    FORESPØRSEL_MOTTATT,
    FORESPOERSEL_BESVART,
    FORESPOERSEL_BESVART_SIMBA,
    FORESPOERSEL_FORKASTET,
    FORESPOERSEL_KASTET_TIL_INFOTRYGD,
    FORESPOERSEL_OPPDATERT,
    FORESPOERSEL_FOR_VEDTAKSPERIODE_ID,
    FORESPOERSEL_LISTE_FOR_PERSON, // Ignoreres av LPS-API
    FORESPOERSEL_MANUELT_FORKASTET, // Ignoreres av LPS-API
}
