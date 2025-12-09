package no.nav.helsearbeidsgiver.plugins

import kotlinx.serialization.Serializable

@Serializable
class ErrorResponse(
    val feilmelding: String,
)
