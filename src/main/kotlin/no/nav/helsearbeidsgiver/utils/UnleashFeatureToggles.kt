package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env

class UnleashFeatureToggles(
    isLocalEnv: Boolean,
) {
    private val unleashClient: Unleash =
        if (isLocalEnv) {
            FakeUnleash()
        } else {
            DefaultUnleash(
                UnleashConfig
                    .builder()
                    .appName("sykepenger-im-lps-api")
                    .instanceId("sykepenger-im-lps-api")
                    .unleashAPI(Env.getProperty("UNLEASH_SERVER_API_URL") + "/api")
                    .apiKey(Env.getProperty("UNLEASH_SERVER_API_TOKEN"))
                    .environment(Env.getProperty("UNLEASH_SERVER_API_ENV"))
                    .build(),
            )
        }

    fun skalOppretteDialogVedMottattSykmelding(): Boolean =
        unleashClient.isEnabled(
            "opprett-dialog-ved-mottatt-sykmelding",
            false,
        )
}
