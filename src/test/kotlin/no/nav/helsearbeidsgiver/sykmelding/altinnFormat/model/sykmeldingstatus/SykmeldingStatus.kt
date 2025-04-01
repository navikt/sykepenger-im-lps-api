package no.nav.syfo.model.sykmeldingstatus

data class ArbeidsgiverStatusDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String? = null,
    val orgNavn: String,
)

data class SporsmalOgSvarDTO(
    val tekst: String,
    val shortName: ShortNameDTO,
    val svartype: SvartypeDTO,
    val svar: String,
)

enum class ShortNameDTO {
    ARBEIDSSITUASJON,
    NY_NARMESTE_LEDER,
    FRAVAER,
    PERIODE,
    FORSIKRING,
    EGENMELDINGSDAGER,
}

enum class SvartypeDTO {
    ARBEIDSSITUASJON,
    PERIODER,
    JA_NEI,
    DAGER,
}
