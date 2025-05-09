package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import java.util.UUID

data class LagreSoknad(
    val soknadId: UUID,
    val sykmeldingId: UUID,
    val fnr: String,
    val orgnr: String,
    val sykepengesoknad: SykepengesoknadDTO,
)
