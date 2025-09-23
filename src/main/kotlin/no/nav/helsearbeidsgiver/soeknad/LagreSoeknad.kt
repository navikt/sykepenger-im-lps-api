package no.nav.helsearbeidsgiver.soeknad

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import java.util.UUID

data class LagreSoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val fnr: String,
    val orgnr: String,
    val sykepengesoeknad: SykepengeSoeknadKafkaMelding,
)
