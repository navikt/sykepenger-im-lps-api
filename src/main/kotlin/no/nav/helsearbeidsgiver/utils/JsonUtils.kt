package no.nav.helsearbeidsgiver.utils

import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.utils.json.jsonConfig

val jsonMapper =
    Json {
        jsonConfig
        ignoreUnknownKeys = true
    }
