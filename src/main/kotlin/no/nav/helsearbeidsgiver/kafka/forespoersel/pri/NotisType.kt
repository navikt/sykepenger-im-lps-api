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
}
