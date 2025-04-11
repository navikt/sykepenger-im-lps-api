package no.nav.helsearbeidsgiver.sykmelding

import kotlinx.serialization.Serializable

@Serializable
data class SykmeldingDTO(
    val id: String,
    val fnr: String,
    val orgnr: String,
    val arbeidsgiverSykmeldingKafka: ArbeidsgiverSykmeldingKafka,
)
