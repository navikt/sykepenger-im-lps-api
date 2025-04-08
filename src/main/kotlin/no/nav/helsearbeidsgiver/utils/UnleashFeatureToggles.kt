package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env

class UnleashFeatureToggles {
    private val apiKey = Env.getProperty("UNLEASH_SERVER_API_TOKEN")
    private val apiUrl = Env.getProperty("UNLEASH_SERVER_API_URL") + "/api"
    private val apiEnv = Env.getProperty("UNLEASH_SERVER_API_ENV")

    private val config: UnleashConfig =
        UnleashConfig
            .builder()
            .appName("sykepenger-im-lps-api")
            .instanceId("sykepenger-im-lps-api")
            .unleashAPI(apiUrl)
            .apiKey(apiKey)
            .environment(apiEnv)
            .build()

    private val unleash: Unleash = DefaultUnleash(config)

    fun skalOppretteDialogVedMottattSykmelding(): Boolean =
        unleash.isEnabled(
            "opprett-dialog-ved-mottatt-sykmelding",
            false,
        )
}
