package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

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
            false,
        )

    fun skalKonsumereSykepengesoeknader(): Boolean =
        unleashClient.isEnabled(
            "konsumer-sykepengesoknader",
            false,
        )

    fun skalKonsumereInntektsmeldinger(): Boolean =
        unleashClient.isEnabled(
            "konsumer-inntektsmeldinger",
            false,
        )

    fun skalKonsumereForespoersler(): Boolean =
        unleashClient.isEnabled(
            "konsumer-forespoersler",
            false,
        )

    fun skalEksponereSykepengesoeknader(orgnr: Orgnr): Boolean =
        unleashClient.isEnabled(
            "eksponer-soeknad-i-api",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalEksponereInntektsmeldinger(): Boolean =
        unleashClient.isEnabled(
            "eksponer-inntektsmeldinger-i-api",
            false,
        )

    fun skalEksponereForespoersler(): Boolean =
        unleashClient.isEnabled(
            "eksponer-forespoersler-i-api",
            false,
        )

    fun skalEksponereSykmeldinger(orgnr: Orgnr): Boolean =
        unleashClient.isEnabled(
            "eksponer-sykmelding-i-api",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalKonsumereStatusISpeil(): Boolean =
        unleashClient.isEnabled(
            "konsumer-status-i-speil",
            false,
        )
}
