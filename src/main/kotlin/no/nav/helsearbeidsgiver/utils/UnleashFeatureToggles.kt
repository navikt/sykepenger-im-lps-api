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

    fun skalOppretteDialogVedMottattSykmelding(orgnr: Orgnr): Boolean =
        unleashClient.isEnabled(
            "opprett-dialog-ved-mottatt-sykmelding",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppdatereDialogVedMottattSoeknad(orgnr: Orgnr): Boolean =
        unleashClient.isEnabled(
            "oppdater-dialog-ved-mottatt-soknad",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr: Orgnr): Boolean =
        unleashClient.isEnabled(
            "forespor-inntektsmelding-via-dialogporten",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppdatereDialogVedMottattInntektsmelding(orgnr: String): Boolean =
        unleashClient.isEnabled(
            "oppdater-dialog-ved-mottatt-inntektsmelding",
            UnleashContext.builder().addProperty("orgnr", orgnr).build(),
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

    fun skalKonsumereSykepengesoeknader(): Boolean =
        unleashClient.isEnabled(
            "konsumer-sykepengesoknader",
            false,
        )

    // TODO: Skal fjernes så fort vi får juridisk avklaring på at det er OK å eksponere
    fun skalEksponereSykepengesoeknader(): Boolean =
        unleashClient.isEnabled(
            "eksponer-soeknad-i-api",
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

    fun skalKonsumereAvvisteInntektsmeldinger(): Boolean =
        unleashClient.isEnabled(
            "konsumer-avviste-inntektsmeldinger",
            false,
        )
}
