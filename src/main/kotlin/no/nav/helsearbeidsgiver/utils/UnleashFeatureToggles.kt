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
                    .build(),
            )
        }

    fun skalOppretteDialogVedMottattSykmelding(orgnr: Orgnr): Boolean =
        unleashClient.isEnabled(
            "opprett-dialog-ved-mottatt-sykmelding",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppdatereDialogVedMottattSoknad(orgnr: Orgnr): Boolean =
        unleashClient.isEnabled(
            "oppdater-dialog-ved-mottatt-soknad",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalSendeApiInnsendteImerTilSimba(): Boolean =
        unleashClient.isEnabled(
            "send-api-innsendte-imer-til-simba",
            false,
        )

    fun skalKonsumereSykmeldinger(): Boolean =
        unleashClient.isEnabled(
            "konsumer-sykmeldinger",
            false,
        )

    fun skalKonsumereSykepengesoknader(): Boolean =
        unleashClient.isEnabled(
            "konsumer-sykepengesoknader",
            false,
        )
}
