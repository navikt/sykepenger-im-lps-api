package no.nav.helsearbeidsgiver.sykmelding

// @file:UseSerializers(UuidSerializer::class)

import kotlinx.serialization.Serializable

// TODO: Gjør om formatet til å være likt som Altinn XML formatet i dagens system
@Serializable
data class SykmeldingResponse(
    val id: String,
    val fnr: String,
    val orgnr: String,
//    val arbeidsgiverSykmelding: ArbeidsgiverSykmelding,
)
