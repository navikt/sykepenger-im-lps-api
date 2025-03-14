package no.nav.helsearbeidsgiver.bakgrunnsjobb

import kotlinx.serialization.json.Json
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class InnsendingProcessor(
    val innsendingService: InnsendingService,
) : BakgrunnsjobbProsesserer {
    companion object {
        const val JOB_TYPE = "innsendingsjobb"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        sikkerLogger().info("Bakgrunnsjobb: Data: $jobb")
        val skjemaInntektsmelding = Json.decodeFromString<SkjemaInntektsmelding>(jobb.data)
        sikkerLogger().info("Skjema Inntektsmelding Data: $skjemaInntektsmelding")
        innsendingService.sendInn(skjemaInntektsmelding)
    }
}
