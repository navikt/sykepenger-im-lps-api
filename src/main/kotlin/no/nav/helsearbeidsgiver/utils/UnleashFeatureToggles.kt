package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env

class UnleashFeatureToggles {
    private val apiKey = Env.getProperty("UNLEASH_SERVER_API_TOKEN")
    private val apiUrl = Env.getProperty("UNLEASH_SERVER_API_URL") + "/api"
    private val apiEnv = Env.getProperty("UNLEASH_SERVER_API_ENV")

    private val defaultUnleash: DefaultUnleash =
        DefaultUnleash(
            UnleashConfig
                .builder()
                .appName("sykepenger-im-lps-api")
                .instanceId("sykepenger-im-lps-api")
                .unleashAPI(apiUrl)
                .apiKey(apiKey)
                .environment(apiEnv)
                .build(),
        )

    fun skalOppretteDialogVedMottattSykmelding(): Boolean =
        defaultUnleash.isEnabled(
            "opprett-dialog-ved-mottatt-sykmelding",
            false,
        )
}
