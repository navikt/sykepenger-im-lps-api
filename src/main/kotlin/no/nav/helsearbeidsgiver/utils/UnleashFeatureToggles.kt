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
                    .fetchTogglesInterval(15)
                    .build(),
            )
        }

    fun skalKonsumereSykmeldinger(): Boolean =
        unleashClient.isEnabled(
            "konsumer-sykmeldinger",
            true,
        )

    fun skalKonsumereSykepengesoeknader(): Boolean =
        unleashClient.isEnabled(
            "konsumer-sykepengesoknader",
            true,
        )

    fun skalKonsumereInntektsmeldinger(): Boolean =
        unleashClient.isEnabled(
            "konsumer-inntektsmeldinger",
            true,
        )

    fun skalKonsumereForespoersler(): Boolean =
        unleashClient.isEnabled(
            "konsumer-forespoersler",
            true,
        )

    fun skalKonsumereStatusISpeil(): Boolean =
        unleashClient.isEnabled(
            "konsumer-status-i-speil",
            true,
        )

    fun skalLagreVedtakArbeidsgiver(): Boolean =
        unleashClient.isEnabled(
            "lagre-vedtak-arbeidsgiver",
            false,
        )
}
