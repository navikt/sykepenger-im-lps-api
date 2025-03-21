package no.nav.helsearbeidsgiver.bakgrunnsjobb

import kotlinx.serialization.json.Json
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.Innsending
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
        val innsending = Json.decodeFromString<Innsending>(jobb.data)
        sikkerLogger().debug("Bakgrunnsjobb: sender inn Skjema Inntektsmelding Data: {}", innsending)
        innsendingService.sendInn(innsending)
    }
}
