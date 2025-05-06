package no.nav.helsearbeidsgiver.kafka.soknad

import kotlinx.serialization.Serializable

@Serializable
data class SykepengesoknadDTO(
    val id: String,
    val type: String,
    val status: String,
    val fnr: String,
    val sykmeldingId: String? = null,
    val arbeidsgiver: ArbeidsgiverDTO? = null,
    // TODO legg til resten av feltene
)

@Serializable
data class ArbeidsgiverDTO(
    val navn: String? = null,
    val orgnummer: String? = null
)
