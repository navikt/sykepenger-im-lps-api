package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class UnleashFeatureToggles {
    private val apiKey = Env.getProperty("UNLEASH_SERVER_API_TOKEN")
    private val apiUrl = Env.getProperty("UNLEASH_SERVER_API_URL") + "/api"
    private val apiEnv = Env.getProperty("UNLEASH_SERVER_API_ENV")

    private val defaultUnleash: DefaultUnleash

    init {
        defaultUnleash =
            DefaultUnleash(
                UnleashConfig
                    .builder()
                    .appName("sykepenger-im-lps-api")
                    .instanceId("sykepenger-im-lps-api")
                    .unleashAPI(apiUrl)
                    .apiKey(apiKey)
                    .environment(apiEnv)
                    .build(),
            ).also { sikkerLogger().info("Unleash koblet opp apiUrl: $apiUrl and apiEnv: $apiEnv") }
    }

    fun skalOppretteDialogVedMottattSykmelding(): Boolean =
        defaultUnleash.isEnabled(
            "opprett-dialog-ved-mottatt-sykmelding",
        )
}
