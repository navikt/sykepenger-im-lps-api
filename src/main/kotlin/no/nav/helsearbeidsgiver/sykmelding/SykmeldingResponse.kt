package no.nav.helsearbeidsgiver.sykmelding

import kotlinx.serialization.Serializable

@Serializable
data class SykmeldingResponse(
    val id: String,
    val fnr: String,
    val orgnr: String,
    val arbeidsgiverSykmelding: ArbeidsgiverSykmelding,
)
